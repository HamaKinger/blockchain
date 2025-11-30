package com.freedom.chain.model.ledger;

import lombok.Data;

/**
 * @description: 交易_输入内部类（UTXO模型核心）
 * @author: freedom
 * @date: 2025/11/21 23:37
 **/
@Data
public class UtxoInput {
    /**
     * 用的上一笔交易哈希
     */
    private String prevTxHash;
    /**
     * 上一笔交易输出索引
     */
    private int prevOutIndex;
    /**
     * 解锁脚本（签名+公钥）
     */
    private String unlockScript;
    /**
     * 交易替换标识
     */
    private long sequence = 0xFFFFFFFFL;

    /**
     * @description: 仅返回参与哈希计算的字段（排除签名相关的动态内容）
     * @author: freedom
     * @date: 2025/11/22 11:12
     * @param: []
     * @return: java.lang.String
     **/
    public String toHashString() {
        // 只包含：prevTxHash、prevOutIndex、sequence（固定字段，不包含 unlockScript 或仅包含公钥部分）
        // 注意：如果 unlockScript 中包含公钥，需要提取公钥部分参与哈希（避免公钥被篡改）
        String publicKeyPart = extractPublicKeyFromUnlockScript(unlockScript); // 提取公钥（后面实现）
        return prevTxHash + ":" + prevOutIndex + ":" + sequence + ":" + publicKeyPart;
    }

    /**
     * @description: 从 unlockScript 中提取公钥（假设格式是“OP_PUSHDATA1 公钥 OP_PUSHDATA1 ”）
     * @author: freedom
     * @date: 2025/11/22 11:12
     * @param: [unlockScript]
     * @return: java.lang.String
     **/
    private String extractPublicKeyFromUnlockScript(String unlockScript) {
        if (unlockScript == null || unlockScript.isEmpty()) {
            return "";
        }
        String[] parts = unlockScript.split(" ");
        // 按我们的脚本格式，公钥在第2位（索引1）：OP_PUSHDATA1 [公钥] OP_PUSHDATA1
        return parts.length >= 2 ? parts[1] : "";
    }
}