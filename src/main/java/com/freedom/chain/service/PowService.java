package com.freedom.chain.service;

import com.alibaba.fastjson2.JSON;
import com.freedom.chain.model.block.Block;
import com.freedom.chain.model.block.BlockCache;
import com.freedom.chain.model.ledger.CoinbaseTransaction;
import com.freedom.chain.model.ledger.PublicLedgerTransaction;
import com.freedom.chain.model.ledger.UtxoInput;
import com.freedom.chain.model.p2p.Message;
import com.freedom.chain.utils.BlockConstant;
import com.freedom.chain.utils.LedgerUtil;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/**
 * @description: 工作量
 * @author: freedom
 * @create: 2025-11-19
 **/
@Service
@Slf4j
public class PowService {
    @Resource
    private BlockCache blockCache;

    @Resource
    private BlockService blockService;

    @Resource
    private P2PService p2PService;

    /**
     * @description: 通过“挖矿”进行工作量证明，实现节点间的共识
     * @author: freedom
     * @date: 2025/11/19 23:37
     * @param:
     * @return:
     **/
    public Block mine() {
        // 1. 动态调整难度（基于最近N个区块的出块时间）
        adjustDifficultyOptimized();
        // 2. 准备交易数据（过滤、排序、限制大小）
        List<PublicLedgerTransaction> blockTransactions = prepareValidTransactions();
        if (blockTransactions.isEmpty()) {
            log.warn("没有可打包的有效交易，放弃挖矿");
            return null;
        }
        // 3. 执行工作量证明
        MiningResult result = proofOfWork(blockTransactions);
        if (result == null) {
            log.error("挖矿失败");
            return null;
        }
        // 4. 创建并广播区块
        return createAndBroadcastBlock(result, blockTransactions);
    }

    /**
     * @description: 动态调整难度
     * @author: freedom
     * @date: 2025/11/22 11:40
     * @param: []
     * @return: void
     **/
    private void adjustDifficultyOptimized() {
        List<Block> blockchain = blockService.getBlockChain();
        int chainSize = blockchain.size();

        // 至少需要11个区块（10个间隔）才能计算平均时间
        if (chainSize < BlockConstant.DIFFICULTY_ADJUST_WINDOW + 1) {
            return;
        }

        // 取最近10个区块的出块时间间隔
        long totalTime = 0;
        int startIndex = chainSize - BlockConstant.DIFFICULTY_ADJUST_WINDOW - 1;
        for (int i = startIndex + 1; i <= chainSize - 1; i++) {
            totalTime += blockchain.get(i).getTimestamp() - blockchain.get(i - 1).getTimestamp();
        }

        long averageTime = totalTime / BlockConstant.DIFFICULTY_ADJUST_WINDOW;
        int currentDifficulty = blockService.getDifficulty();
        int newDifficulty = currentDifficulty;

        // 根据平均时间调整难度（目标10分钟/块）
        if (averageTime < BlockConstant.EXPECTED_BLOCK_TIME_MS * 0.8) {
            newDifficulty++; // 出块过快，提高难度
        } else if (averageTime > BlockConstant.EXPECTED_BLOCK_TIME_MS * 1.2) {
            newDifficulty = Math.max(1, currentDifficulty - 1); // 出块过慢，降低难度（最低为1）
        }

        if (newDifficulty != currentDifficulty) {
            blockCache.setDifficulty(newDifficulty);
            log.info("难度调整: {} -> {} (平均出块时间: {}ms)",
                    currentDifficulty, newDifficulty, averageTime);
        }
    }

    /**
     * @description: 准备交易数据（过滤、排序、限制大小）
     * @author: freedom
     * @date: 2025/11/22 11:36
     * @param: []
     * @return: java.util.List<com.freedom.chain.model.ledger.PublicLedgerTransaction>
     **/
    private List<PublicLedgerTransaction> prepareValidTransactions() {
        List<PublicLedgerTransaction> candidateTxs = Lists.newArrayList();
        Block latestBlock = blockCache.getLatestBlock();
        long blockHeight = latestBlock != null ? latestBlock.getIndex() + 1 : 1;

        // 1. 添加Coinbase交易（挖矿奖励）
        CoinbaseTransaction coinbaseTx = createCoinbaseTransaction(blockHeight);
        candidateTxs.add(coinbaseTx);
        long currentBlockSize = estimateTransactionSize(coinbaseTx);

        // 2. 筛选并排序待处理交易
        List<PublicLedgerTransaction> validPendingTxs = blockCache.getPackedTransactions().stream()
                .filter(this::isValidTransaction) // 验证交易有效性
                .sorted(Comparator.comparingDouble(this::calculateTxPriority).reversed()) // 按优先级排序
                .toList();

        // 3. 填充区块（不超过最大区块大小）
        for (PublicLedgerTransaction tx : validPendingTxs) {
            long txSize = estimateTransactionSize(tx);
            if (currentBlockSize + txSize > BlockConstant.MAX_BLOCK_SIZE) {
                log.info("区块大小已达上限，停止添加交易");
                break;
            }
            candidateTxs.add(tx);
            currentBlockSize += txSize;
        }

        return candidateTxs;
    }

    /**
     * @description: 按优先级排序
     * @author: freedom
     * @date: 2025/11/22 11:36
     * @param: [tx]
     * @return: double
     **/
    private double calculateTxPriority(PublicLedgerTransaction tx) {
        long txSize = estimateTransactionSize(tx);
        if (txSize == 0) return 0;

        // 基础优先级：手续费率（聪/字节）
        double feeRate = new BigDecimal(tx.getFee())
                .divide(new BigDecimal(txSize), 10, BigDecimal.ROUND_HALF_UP)
                .doubleValue();

        // 增加交易年龄权重（未确认时间越长优先级越高）
        long age = System.currentTimeMillis() - tx.getTimestamp();
        return feeRate * (1 + Math.min(age / (24 * 60 * 60 * 1000), 7)); // 最大7天加权
    }

    /**
     * @description: 验证交易有效性
     * @author: freedom
     * @date: 2025/11/22 11:36
     * @param: [tx]
     * @return: boolean
     **/
    private boolean isValidTransaction(PublicLedgerTransaction tx) {
        if (!tx.verify()) {
            log.warn("交易签名验证失败: {}", tx.getTxHash());
            return false;
        }
        return true;
    }

    /**
     * @description: 执行工作量证明
     * @author: freedom
     * @date: 2025/11/22 11:40
     * @param: [transactions]
     * @return: com.freedom.chain.service.PowService.MiningResult
     **/
    private MiningResult proofOfWork(List<PublicLedgerTransaction> transactions) {
        Block latestBlock = blockCache.getLatestBlock();
        String previousHash = latestBlock != null ? latestBlock.getHash() : "";
        long timestamp = System.currentTimeMillis();
        int nonce = 0;
        String target = new String(new char[blockService.getDifficulty()]).replace('\0', '0');

        log.info("开始挖矿，难度: {}，目标前缀: {}", blockService.getDifficulty(), target);
        long start = System.currentTimeMillis();

        while (true) {
            // 检查是否有新块被添加（避免无效挖矿）
            if (latestBlock != null && blockCache.getLatestBlock().getIndex() > latestBlock.getIndex()) {
                log.info("检测到新块已生成，终止当前挖矿");
                return null;
            }

            String hash = blockService.calculateHash(previousHash, timestamp, transactions, nonce);
            if (hash.startsWith(target)) {
                long elapsed = System.currentTimeMillis() - start;
                log.info("挖矿成功，耗时: {}ms, nonce: {}, hash: {}", elapsed, nonce, hash);
                return new MiningResult(hash, nonce, timestamp, elapsed);
            }

            nonce++;
            // 每10000次尝试检查一次是否需要退出
            if (nonce % 10000 == 0 && Thread.currentThread().isInterrupted()) {
                log.info("挖矿被中断，当前nonce: {}", nonce);
                return null;
            }
        }
    }

    /**
     * @description: 创建并广播区块
     * @author: freedom
     * @date: 2025/11/22 11:40
     * @param: [result, transactions]
     * @return: com.freedom.chain.model.block.Block
     **/
    private Block createAndBroadcastBlock(MiningResult result, List<PublicLedgerTransaction> transactions) {
        Block newBlock = blockService.createNewBlock(
                result.nonce,
                blockCache.getLatestBlock().getHash(),
                result.hash,
                result.timestamp,
                transactions
        );

        if (newBlock != null) {
            // 标记交易为已打包并更新UTXO
            updateUtxos(transactions);
            blockCache.getPackedTransactions().removeAll(transactions);

            // 广播新区块
            Message msg = new Message();
            msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
            msg.setData(JSON.toJSONString(newBlock));
            p2PService.broatcast(JSON.toJSONString(msg));

            log.info("区块创建成功，高度: {}", newBlock.getIndex());
        }
        return newBlock;
    }

    /**
     * @description: 标记交易为已打包并更新UTXO
     * @author: freedom
     * @date: 2025/11/22 11:37
     * @param: [transactions]
     * @return: void
     **/
    private void updateUtxos(List<PublicLedgerTransaction> transactions) {
        for (PublicLedgerTransaction tx : transactions) {
            // 标记输入的UTXO为已花费
            for (UtxoInput input : tx.getUtxoInputs()) {
                LedgerUtil.markUtxoAsSpent(input.getPrevTxHash(), input.getPrevOutIndex());
            }
            // 添加输出的UTXO
            LedgerUtil.addUtxos(tx.getTxHash(), tx.getUtxoOutputs());
        }
        // 先内存，后文件：更新完成后保存快照
        LedgerUtil.saveUtxoSnapshot();
    }

    /**
     * @description: 添加Coinbase交易（挖矿奖励）
     * @author: freedom
     * @date: 2025/11/22 11:40
     * @param: [blockHeight]
     * @return: com.freedom.chain.model.ledger.CoinbaseTransaction
     **/
    private CoinbaseTransaction createCoinbaseTransaction(long blockHeight) {
        // 使用当前节点的矿工地址（实际应从配置或密钥对中获取）
        String minerAddress = blockCache.getMinerAddress();
        if (minerAddress == null || minerAddress.isEmpty()) {
            blockCache.setMinerAddress(minerAddress);
        }
        return new CoinbaseTransaction(minerAddress, blockHeight);
    }

    /**
     * @description: 预估数据大小
     * @author: freedom
     * @date: 2025/11/22 11:39
     * @param: [tx]
     * @return: long
     **/
    private long estimateTransactionSize(PublicLedgerTransaction tx) {
        return JSON.toJSONString(tx).getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * @description: 工作量证明
     * @author: freedom
     * @date: 2025/11/22 11:39
     **/
    private static class MiningResult {
        String hash;
        int nonce;
        long timestamp;
        long elapsedTime;

        MiningResult(String hash, int nonce, long timestamp, long elapsedTime) {
            this.hash = hash;
            this.nonce = nonce;
            this.timestamp = timestamp;
            this.elapsedTime = elapsedTime;
        }
    }

}
