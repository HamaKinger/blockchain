package com.freedom.chain.utils;

import com.freedom.chain.model.ledger.UtxoOutput;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.alibaba.fastjson2.JSON;
import org.springframework.util.ResourceUtils;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

/**
 * @description: 公共账本UTXO管理工具类：模拟区块链节点对UTXO的存储和查询 实际场景中：UTXO数据持久化到数据库（如LevelDB），此处用内存Map模拟
 * @author: freedom
 * @create: 2025-11-21
 **/
public class LedgerUtil {
    // 核心存储：UTXO集合（key=prevTxHash+":"+prevOutIndex，唯一标识一个UTXO；value=UTXO详情）
    private static final Map<String, UtxoEntry> UTXO_STORAGE = new ConcurrentHashMap<>();

    // UTXO详情实体（账本内部存储，不对外暴露）
    @lombok.Data
    private static class UtxoEntry {
        private String prevTxHash; // 所属交易哈希
        private int prevOutIndex; // 输出索引
        private String recipientAddress; // 接收方地址（UTXO归属）
        private BigInteger amount; // 金额（最小单位）
        private boolean isSpent; // 是否已被花费
    }

    /**
     * 新增UTXO（交易打包上链后调用）
     * @param txHash 交易哈希（生成该UTXO的交易）
     * @param outputs 交易输出列表（每个输出对应一个UTXO）
     */
    public static void addUtxos(String txHash, List<UtxoOutput> outputs) {
        for (UtxoOutput output : outputs) {
            String utxoKey = buildUtxoKey(txHash, output.getOutputIndex());
            UtxoEntry entry = new UtxoEntry();
            entry.setPrevTxHash(txHash);
            entry.setPrevOutIndex(output.getOutputIndex());
            entry.setRecipientAddress(output.getRecipientAddress());
            entry.setAmount(output.getAmount());
            entry.setSpent(false); // 新生成的UTXO未被花费
            UTXO_STORAGE.put(utxoKey, entry);
        }
    }

    /**
     * 查询UTXO金额（交易验证时调用，确认输入资金合法性）
     * @param prevTxHash 引用的交易哈希
     * @param prevOutIndex 引用的输出索引
     * @return UTXO金额（不存在则返回0）
     */
    public static BigInteger getUtxoAmount(String prevTxHash, int prevOutIndex) {
        String utxoKey = buildUtxoKey(prevTxHash, prevOutIndex);
        UtxoEntry entry = UTXO_STORAGE.get(utxoKey);
        return entry != null ? entry.getAmount() : BigInteger.ZERO;
    }

    /**
     * 检查UTXO是否已被花费（防双花核心）
     * @param prevTxHash 引用的交易哈希
     * @param prevOutIndex 引用的输出索引
     * @return true=已花费，false=未花费
     */
    public static boolean isUtxoSpent(String prevTxHash, int prevOutIndex) {
        String utxoKey = buildUtxoKey(prevTxHash, prevOutIndex);
        UtxoEntry entry = UTXO_STORAGE.get(utxoKey);
        return entry != null && entry.isSpent();
    }

    /**
     * 标记UTXO为已花费（交易打包上链后调用）
     * @param prevTxHash 引用的交易哈希
     * @param prevOutIndex 引用的输出索引
     */
    public static void markUtxoAsSpent(String prevTxHash, int prevOutIndex) {
        String utxoKey = buildUtxoKey(prevTxHash, prevOutIndex);
        UtxoEntry entry = UTXO_STORAGE.get(utxoKey);
        if (entry != null) {
            entry.setSpent(true);
        }
    }

    /**
     * 查询某个地址的所有未花费UTXO（发起方转账时获取可用资金）
     * @param address 地址
     * @return 未花费UTXO列表
     */
    public static List<UtxoEntry> getUnspentUtxosByAddress(String address) {
        return UTXO_STORAGE.values().stream()
                .filter(entry -> address.equals(entry.getRecipientAddress()) && !entry.isSpent())
                .collect(Collectors.toList());
    }
    
    /**
     * 按地址获取UTXO（按交易哈希分组）
     * @param address 地址
     * @u8fd4回 Map<交易哈希, UTXO输出列表>
     */
    public static Map<String, List<UtxoOutput>> getUtxosByAddress(String address) {
        return UTXO_STORAGE.values().stream()
                .filter(entry -> address.equals(entry.getRecipientAddress()) && !entry.isSpent())
                .collect(Collectors.groupingBy(
                    UtxoEntry::getPrevTxHash,
                    Collectors.mapping(entry -> {
                        UtxoOutput output = new UtxoOutput();
                        output.setOutputIndex(entry.getPrevOutIndex());
                        output.setRecipientAddress(entry.getRecipientAddress());
                        output.setAmount(entry.getAmount());
                        return output;
                    }, Collectors.toList())
                ));
    }

    /**
     * 计算某个地址的余额（未花费UTXO金额总和）
     * @param address 地址
     * @return 账户余额
     */
    public static BigInteger getAddressBalance(String address) {
        return getUnspentUtxosByAddress(address).stream()
                .map(UtxoEntry::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * 构建UTXO唯一键（prevTxHash + 分隔符 + prevOutIndex）
     */
    private static String buildUtxoKey(String prevTxHash, int prevOutIndex) {
        return prevTxHash + ":" + prevOutIndex;
    }

    // 测试用：清空UTXO存储（仅用于单元测试）
    public static void clearUtxos() {
        UTXO_STORAGE.clear();
    }
    
    /**
     * 保存UTXO快照到本地文件（file/utxo.json）
     */
    public static void saveUtxoSnapshot() {
        try {
            File file = ResourceUtils.getFile("file/utxo.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(JSON.toJSONString(UTXO_STORAGE.values()));
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * 从本地文件加载UTXO快照到内存
     */
    public static void loadUtxoSnapshot() {
        try {
            File file = ResourceUtils.getFile("file/utxo.json");
            if (!file.exists() || file.length() == 0) {
                return;
            }
            String content = new String(Files.readAllBytes(file.toPath()));
            if (content == null || content.trim().isEmpty()) {
                return;
            }
            List<UtxoEntry> list = JSON.parseArray(content, UtxoEntry.class);
            UTXO_STORAGE.clear();
            for (UtxoEntry entry : list) {
                String key = buildUtxoKey(entry.getPrevTxHash(), entry.getPrevOutIndex());
                UTXO_STORAGE.put(key, entry);
            }
        } catch (Exception ignored) {
        }
    }
}
