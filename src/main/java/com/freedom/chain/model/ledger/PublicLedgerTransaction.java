package com.freedom.chain.model.ledger;

import com.freedom.chain.enumst.TransactionStatus;
import com.freedom.chain.utils.CryptoUtil;
import com.freedom.chain.utils.LedgerUtil;
import com.freedom.chain.utils.SignatureUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @description: 公共账本UTXO模型交易实体（适配比特币风格公共账本）
 * @author: freedom
 * @date: 2025/11/21 23:37
 **/
@Data
@Slf4j
public class PublicLedgerTransaction {
    /**
     * 模块1：身份与标识 交易哈希（全网唯一）
     */
    private String txHash;
    /**
     * 发起方地址（全零表示Coinbase交易）
     */
    private String fromAddress;
    /**
     * 接收方地址（null表示合约创建）
     */
    private String toAddress;
    /**
     * 协议版本
     */
    private int version = 1;
    /**
     * 时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 模块2：UTXO资金流转  交易输入（引用UTXO）
     */
    private List<UtxoInput> utxoInputs;
    /**
     * 交易输出（生成新UTXO）
     */
    private List<UtxoOutput> utxoOutputs;
    /**
     * 交易手续费（自动计算）
     */
    private BigInteger fee;

    /**
     * 模块3：安全与共识 发起方签名（ECDSA）
     */
    private byte[] signature;
    /**
     * 交易状态（本地维护）
     */
    private TransactionStatus status;
    /**
     * 链标识（主网默认1）
     */
    private int chainId = 1;

    /**
     * 模块4：扩展字段 公开备注
     */
    private String memo;

    /**
     * 模块5：扩展字段 公钥地址
     */
    private String publicKey;

    /**
     * ------------------- 公共账本核心方法 -------------------
     * @description: 成交易哈希（txHash）：基于核心字段，确保不可篡改
     * @author: freedom
     * @date: 2025/11/21 23:42
     * @param:
     * @return:
     **/
    public void generateTxHash() {
        // 哈希源：不含签名（签名后哈希不变，保证验证一致性）
        // 哈希源：仅包含核心固定字段，用 UtxoInput.toHashString() 排除签名
        String hashSource = version +
                fromAddress +
                toAddress +
                timestamp +
                chainId +
                memo +
                utxoInputs.stream().map(UtxoInput::toHashString).collect(Collectors.joining(",")) +
                utxoOutputs.stream().map(UtxoOutput::toString).collect(Collectors.joining(","));
        this.txHash = CryptoUtil.sha256(hashSource); // 自定义SHA-256工具类
    }


    /**
     * @description: 计算交易手续费（UTXO模型隐含手续费）
     * @author: freedom
     * @date: 2025/11/21 23:44
     * @param:
     * @return:
     **/
    public void calculateFee() {
        BigInteger totalInput = utxoInputs.stream()
                .map(input -> {
                    // 从账本中查询prevTxHash对应的UTXO金额（实际需遍历区块）
                    return LedgerUtil.getUtxoAmount(input.getPrevTxHash(), input.getPrevOutIndex());
                })
                .reduce(BigInteger.ZERO, BigInteger::add);

        BigInteger totalOutput = utxoOutputs.stream()
                .map(UtxoOutput::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        this.fee = totalInput.subtract(totalOutput);
        log.info("交易验证 - Hash: {}, 总输入: {}, 总输出: {}, 手续费: {}",
                txHash, totalInput, totalOutput, fee);
        if (fee.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("交易手续费不能为负");
        }
    }


    /**
     * @description: 交易签名（发起方用私钥签名，确保交易合法性）
     * @author: freedom
     * @date: 2025/11/21 23:44
     * @param:
     * @return:
     **/
    public void sign(PrivateKey privateKey) {
        // 对交易哈希签名（减少签名数据量，提高验证效率）
        byte[] hashBytes = CryptoUtil.sha256Bytes(txHash.getBytes());
        this.signature = SignatureUtil.ecdsaSign(hashBytes, privateKey); // 自定义ECDSA工具类
    }


    /**
     * @description: 交易验证（公共账本节点必执行，确保交易合法） 验证逻辑：哈希正确 + 签名合法 + 输入UTXO未被花费 + 手续费合法
     * @author: freedom
     * @date: 2025/11/21 23:44
     * @param:
     * @return:
     **/
    public boolean verify() {
        // ① 验证交易哈希未被篡改
        String tempTxHash = CryptoUtil.sha256(version +
                fromAddress +
                toAddress +
                timestamp +
                chainId +
                memo +
                utxoInputs.stream().map(UtxoInput::toHashString).collect(Collectors.joining(",")) +
                utxoOutputs.stream().map(UtxoOutput::toString).collect(Collectors.joining(",")));
        if (!tempTxHash.equals(txHash)) {
            return false;
        }

        // ② 验证签名合法（用fromAddress对应的公钥验证）
        byte[] hashBytes = CryptoUtil.sha256Bytes(txHash.getBytes());
        if (!SignatureUtil.ecdsaVerify(hashBytes, signature, publicKey)) {
            return false;
        }

        // ③ 验证所有输入UTXO未被花费（查询账本）
        for (UtxoInput utxoInput : utxoInputs) {
            if (LedgerUtil.isUtxoSpent(utxoInput.getPrevTxHash(), utxoInput.getPrevOutIndex())) {
                log.warn("UTXO 被花费, 验证失败,hash:{}",txHash);
                return false;
            }
        }

        // ④ 验证手续费合法（输入≥输出）
        calculateFee();
        return fee.compareTo(BigInteger.ZERO) >= 0;
    }
}

