package com.freedom.chain.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.freedom.chain.enumst.ResultCodeEnum;
import com.freedom.chain.enumst.TransactionStatus;
import com.freedom.chain.error.BusinessException;
import com.freedom.chain.model.block.Block;
import com.freedom.chain.model.block.BlockCache;
import com.freedom.chain.model.ledger.CoinbaseTransaction;
import com.freedom.chain.model.ledger.PublicLedgerTransaction;
import com.freedom.chain.po.SerializableKeyPair;
import com.freedom.chain.utils.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Map;


/**
 * @description: 创世区块
 * @author: freedom
 * @create: 2025-11-19
 **/
@Service
@Slf4j
public class BlockService {

    @Resource
    BlockCache blockCache;

    /**
     * @description: 创建创世区块
     * @author: freedom
     * @date: 2025/11/22 9:52
     * @param: []
     * @return: void
     **/
    public void createGenesisBlock() {
        try {
            // 添加检查，确保只有在区块链为空时才创建创世区块
            if (!blockCache.getBlockChain().isEmpty()) {
                throw new BusinessException(ResultCodeEnum.FAILED,"区块链已存在，创世区块只能创建一次");
            }
            Block genesisBlock = new Block();
            //设置创世区块高度为1
            long blockHeight = 1; // 模拟第1个区块
            genesisBlock.setIndex(Integer.parseInt(String.valueOf(blockHeight)));
            genesisBlock.setTimestamp(System.currentTimeMillis());
            genesisBlock.setNonce(1);
            //封装业务数据
            List<PublicLedgerTransaction> tsaList = generateGenesisTransactions(blockHeight);
            genesisBlock.setTransactions(tsaList);
            //设置创世区块的hash值
            genesisBlock.setHash(calculateHash(genesisBlock.getPreviousHash(),genesisBlock.getTimestamp(),tsaList,1));
            //添加到已打包保存的业务数据集合中
            blockCache.getPackedTransactions().addAll(tsaList);
            //添加到区块链中
            blockCache.getBlockChain().add(genesisBlock);
            log.info("创世区块生成成功: {}", JSON.toJSONString(genesisBlock));
            List<Map<String,Block>> blockDatas = Lists.newArrayList();
            Map<String,Block> map = Maps.newHashMap();
            map.put(genesisBlock.getHash(),genesisBlock);
            blockDatas.add(map);
            //保存到本地
            File file = ResourceUtils.getFile("file/block.json");
            try (FileWriter fileWriter = new FileWriter(file)){
                fileWriter.write(JSON.toJSONString(blockDatas));
                JSON.toJSONString(genesisBlock);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建新区块
     * @param nonce
     * @param previousHash
     * @param hash
     * @param blockTxs
     * @return
     */
    public Block createNewBlock(int nonce, String previousHash, String hash,long start, List<PublicLedgerTransaction> blockTxs) {
        Block block = new Block();
        block.setIndex(blockCache.getBlockChain().size() + 1);
        //时间戳
        block.setTimestamp(start);
        block.setTransactions(blockTxs);
        //工作量证明，计算正确hash值的次数
        block.setNonce(nonce);
        //上一区块的哈希
        block.setPreviousHash(previousHash);
        //当前区块的哈希
        block.setHash(hash);
        if (addBlock(block)) {
            return block;
        }
        return null;
    }

    /**
     * 添加新区块到当前节点的区块链中
     *
     * @param newBlock
     */
    public boolean addBlock(Block newBlock) {
        //先对新区块的合法性进行校验
        if (isValidNewBlock(newBlock, blockCache.getLatestBlock())) {
            blockCache.getBlockChain().add(newBlock);
            // 新区块的业务数据需要加入到已打包的交易集合里去
            blockCache.getPackedTransactions().addAll(newBlock.getTransactions());
            
            // 持久化到本地文件
            saveBlockToFile(newBlock);
            
            return true;
        }
        return false;
    }
    
    /**
     * 将区块保存到本地文件
     * @param block 要保存的区块
     */
    private void saveBlockToFile(Block block) {
        try {
            File file = ResourceUtils.getFile("file/block.json");
            
            // 读取现有区块数据
            List<Map<String, Block>> blockDatas = Lists.newArrayList();
            if (file.exists() && file.length() > 0) {
                String existingContent = new String(Files.readAllBytes(file.toPath()));
                if (!existingContent.trim().isEmpty()) {
                    blockDatas = JSON.parseObject(existingContent,
                        new TypeReference<List<Map<String, Block>>>(){}.getType());
                }
            }
            
            // 添加新区块
            Map<String, Block> map = Maps.newHashMap();
            map.put(block.getHash(), block);
            blockDatas.add(map);
            
            // 写入文件
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(JSON.toJSONString(blockDatas));
                log.info("区块 #{} 已保存到本地文件: {}", block.getIndex(), block.getHash());
            }
        } catch (IOException e) {
            log.error("保存区块到文件失败", e);
            throw new BusinessException(ResultCodeEnum.ERROR, "保存区块到文件失败: " + e.getMessage());
        }
    }

    /**
     * 验证新区块是否有效
     *
     * @param newBlock
     * @param previousBlock
     * @return
     */
    public boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        // 处理创世区块的情况（previousBlock 为 null）
        if (previousBlock == null) {
            // 创世区块的 previousHash 应该为空或特定值
            if (StrUtil.isNotEmpty(newBlock.getPreviousHash())) {
                log.error("创世区块的前一个区块hash应该是空值");
                return false;
            }
            // 验证创世区块hash值的正确性
            String hash = calculateHash(newBlock.getPreviousHash(), newBlock.getTimestamp(),
                    newBlock.getTransactions(), newBlock.getNonce());
            if (!hash.equals(newBlock.getHash())) {
                log.info("创世区块的hash无效: {}, {}", hash, newBlock.getHash());
                return false;
            }
            // 验证hash是否满足难度要求
            return isValidHash(newBlock.getHash());
        }
        if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
            log.info("新区块的前一个区块hash验证不通过");
            return false;
        } else {
            // 验证新区块hash值的正确性
            String hash = calculateHash(newBlock.getPreviousHash(),newBlock.getTimestamp(), newBlock.getTransactions(), newBlock.getNonce());
            log.info("验证新区块hash值的正确性hash: {}, newBlock info:{}" ,hash,JSON.toJSONString(newBlock));
            if (!hash.equals(newBlock.getHash())) {
                return false;
            }
            return isValidHash(newBlock.getHash());
        }
    }

    /**
     * 验证hash值是否满足系统条件
     *
     * @param hash
     * @return
     */
    public boolean isValidHash(String hash) {
        int difficulty = getDifficulty();
        String target = new String(new char[difficulty]).replace('\0', '0'); // 创建目标字符串 "00000"
        return hash.startsWith(target);
    }

    /**
     * 验证整个区块链是否有效
     * @param chain
     * @return
     */
    public boolean isValidChain(List<Block> chain) {
        Block block = null;
        Block lastBlock = chain.get(0);
        int currentIndex = 1;
        while (currentIndex < chain.size()) {
            block = chain.get(currentIndex);

            if (!isValidNewBlock(block, lastBlock)) {
                return false;
            }

            lastBlock = block;
            currentIndex++;
        }
        return true;
    }

    /**
     * 替换本地区块链
     *
     * @param newBlocks
     */
    public void replaceChain(List<Block> newBlocks) {
        List<Block> localBlockChain = blockCache.getBlockChain();
        List<PublicLedgerTransaction> localpackedTransactions = blockCache.getPackedTransactions();
        if (isValidChain(newBlocks) && newBlocks.size() > localBlockChain.size()) {
            localBlockChain = newBlocks;
            //替换已打包保存的业务数据集合
            localpackedTransactions.clear();
            localBlockChain.forEach(block -> {
                localpackedTransactions.addAll(block.getTransactions());
            });
            blockCache.setBlockChain(localBlockChain);
            blockCache.setPackedTransactions(localpackedTransactions);
            log.info("替换后的本节点区块链：{}",JSON.toJSONString(blockCache.getBlockChain()));
        } else {
            log.warn("接收的区块链无效");
        }
    }

    /**
     * @description: 当前节点的socket对象
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public List<WebSocket> getSockets(){
        return blockCache.getSocketsList();
    }

    /**
     * @description: 获取最新的区块
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public Block getLatestBlock() {
       return blockCache.getLatestBlock();
    }

    /**
     * @description: 当前节点p2pserver端口号
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public int getP2pport() {
        return blockCache.getP2pport();
    }

    /**
     * @description: 要连接的节点地址
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public String getAddress() {
        return blockCache.getAddress();
    }

    /**
     * @description: 挖矿的难度系数
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public int getDifficulty() {
        return blockCache.getDifficulty();
    }

    /**
     * @description: 当前节点的区块链结构
     * @author: freedom
     * @date: 2025/11/21 23:19
     * @param:
     * @return:
     **/
    public List<Block> getBlockChain() {
        return blockCache.getBlockChain();
    }

    /**
     * 计算区块的hash
     *
     * @param previousHash
     * @param currentTransactions
     * @param nonce
     * @param timeStamp
     * @return
     */
    public String calculateHash(String previousHash,long timeStamp,
                                List<PublicLedgerTransaction> currentTransactions, int nonce) {
        return CryptoUtil.sha256(previousHash + timeStamp + nonce+ JSON.toJSONString(currentTransactions) );
    }


    /**
     * 生成创世区块的交易记录
     * @param blockHeight 创世区块高度
     * @return 交易列表
     */
    private List<PublicLedgerTransaction> generateGenesisTransactions(long blockHeight) throws IOException {
        List<PublicLedgerTransaction> tsaList = Lists.newArrayList();
        String receiverAddress;
        if(FileUtil.isEmpty("file/mineInfo.json")){
            // 1. 生成发起方密钥对和地址
            KeyPair receiverKeyPair = SignatureUtil.generateKeyPair();
            receiverAddress = AddressUtil.publicKeyToAddress(receiverKeyPair.getPublic());
            Map<String,SerializableKeyPair> keyPairHashMap = Maps.newHashMap();
            SerializableKeyPair serializableKP = new SerializableKeyPair();
            String privateKeyBase64 = Base64.getEncoder().encodeToString(receiverKeyPair.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(receiverKeyPair.getPublic().getEncoded());
            serializableKP.setPrivateKeyBase64(privateKeyBase64);
            serializableKP.setPublicKeyBase64(publicKeyBase64);
            serializableKP.setAlgorithm("RSA"); // 明确算法
            serializableKP.setKeySize(2048);    // 明确密钥长度
            serializableKP.setCurveName(null);  // RSA 非椭圆曲线，设为 null
            keyPairHashMap.put(receiverAddress, serializableKP);
            File file = ResourceUtils.getFile("file/mineInfo.json");
            // 写入 JSON 字符串
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(JSON.toJSONString(keyPairHashMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            String mineInfo = Files.readString(Path.of("file/mineInfo.json"));
            JSONObject mineInfoDatas = JSON.parseObject(mineInfo);
            receiverAddress = mineInfoDatas.keySet().iterator().next();
        }
        //存储地址
        blockCache.setMinerAddress(receiverAddress);
        // 2. 模拟发起方有初始UTXO（如挖矿奖励）
        CoinbaseTransaction tx = new CoinbaseTransaction(receiverAddress, blockHeight);
        // 3. 验证交易合法性（节点打包前执行）
        boolean isTxValid = tx.verify();
        // 4. 交易打包上链（标记输入UTXO为已花费，新增输出UTXO）
        if (isTxValid) {
            tx.setStatus(TransactionStatus.CONFIRMED);
            // 标记输入UTXO为已花费
            tx.getUtxoInputs().forEach(in -> LedgerUtil.markUtxoAsSpent(in.getPrevTxHash(), in.getPrevOutIndex()));
            // 新增输出UTXO
            LedgerUtil.addUtxos(tx.getTxHash(), tx.getUtxoOutputs());
            log.info("创世区块交易打包成功，交易哈希：{}", tx.getTxHash());
        }
        tsaList.add(tx);
        return tsaList;
    }
}
