package com.freedom.chain.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.freedom.chain.dto.TransferRequest;
import com.freedom.chain.enumst.ResultCodeEnum;
import com.freedom.chain.enumst.TransactionStatus;
import com.freedom.chain.error.Assert;
import com.freedom.chain.error.BusinessException;
import com.freedom.chain.model.block.BlockCache;
import com.freedom.chain.model.ledger.PublicLedgerTransaction;
import com.freedom.chain.model.ledger.UtxoInput;
import com.freedom.chain.model.ledger.UtxoOutput;
import com.freedom.chain.po.SerializableKeyPair;
import com.freedom.chain.utils.CryptoUtil;
import com.freedom.chain.utils.LedgerUtil;
import com.freedom.chain.utils.SignatureUtil;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @description: 交易服务
 * @author: freedom
 * @create: 2025-11-28
 **/
@Service
@Slf4j
public class TransactionService {

    @Resource
    private BlockCache blockCache;

    /**
     * 创建转账交易
     * @param request 转账请求
     * @return 交易哈希
     */
    public String createTransfer(TransferRequest request) {
        try {
            // 1. 获取发送方私钥
            SerializableKeyPair senderKeyPair = getSenderKeyPair(request.getFromAddress());
            Assert.notNull(senderKeyPair, "未找到发送方密钥，无法签名交易");

            // 2. 将BigDecimal转换为BigInteger（聪为单位，1 BTC = 100,000,000 聪）
            BigInteger amount = request.getAmount().multiply(new java.math.BigDecimal("100000000")).toBigInteger();
            BigInteger fee = request.getFee().multiply(new java.math.BigDecimal("100000000")).toBigInteger();
            BigInteger totalAmount = amount.add(fee);

            // 3. 查找可用的UTXO
            List<UtxoInput> inputs = Lists.newArrayList();
            BigInteger totalInput = BigInteger.ZERO;

            // 获取发送方的所有可用UTXO
            Map<String, List<UtxoOutput>> utxos = LedgerUtil.getUtxosByAddress(request.getFromAddress());
            
            // 遍历所有交易的UTXO
            for (Map.Entry<String, List<UtxoOutput>> entry : utxos.entrySet()) {
                String txHash = entry.getKey();
                List<UtxoOutput> outputs = entry.getValue();
                
                for (int i = 0; i < outputs.size(); i++) {
                    UtxoOutput output = outputs.get(i);
                    if (output.getRecipientAddress().equals(request.getFromAddress())) {
                        // 创建输入
                        UtxoInput input = new UtxoInput();
                        input.setPrevTxHash(txHash);
                        input.setPrevOutIndex(i);
                        inputs.add(input);
                        
                        totalInput = totalInput.add(output.getAmount());
                        
                        // 如果已经足够支付，停止收集
                        if (totalInput.compareTo(totalAmount) >= 0) {
                            break;
                        }
                    }
                }
                
                if (totalInput.compareTo(totalAmount) >= 0) {
                    break;
                }
            }

            // 4. 检查余额是否足够
            if (totalInput.compareTo(totalAmount) < 0) {
                throw new BusinessException(ResultCodeEnum.FAILED, 
                    "余额不足，当前可用: " + totalInput + " 聪，需要: " + totalAmount + " 聪");
            }

            // 5. 创建输出
            List<UtxoOutput> outputs = Lists.newArrayList();
            
            // 给接收方的输出
            UtxoOutput receiverOutput = new UtxoOutput();
            receiverOutput.setRecipientAddress(request.getToAddress());
            receiverOutput.setAmount(amount);
            receiverOutput.setOutputIndex(0);
            outputs.add(receiverOutput);

            // 找零输出（如果有）
            BigInteger change = totalInput.subtract(totalAmount);
            if (change.compareTo(BigInteger.ZERO) > 0) {
                UtxoOutput changeOutput = new UtxoOutput();
                changeOutput.setRecipientAddress(request.getFromAddress());
                changeOutput.setAmount(change);
                changeOutput.setOutputIndex(1);
                outputs.add(changeOutput);
            }

            // 6. 创建交易对象
            PublicLedgerTransaction transaction = new PublicLedgerTransaction();
            transaction.setVersion(1);
            transaction.setFromAddress(request.getFromAddress());
            transaction.setToAddress(request.getToAddress());
            transaction.setTimestamp(System.currentTimeMillis());
            transaction.setUtxoInputs(inputs);
            transaction.setUtxoOutputs(outputs);
            transaction.setFee(fee);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setChainId(1);
            transaction.setMemo("Transfer");
            byte[] decodePrivateKey = Base64.getDecoder().decode(senderKeyPair.getPrivateKeyBase64());
            byte[] decodePublicKey = Base64.getDecoder().decode(senderKeyPair.getPublicKeyBase64());
            transaction.setPublicKey(CryptoUtil.bytesToHex(decodePublicKey));

            // 7. 生成交易哈希
            transaction.generateTxHash();

            // 8. 签名交易
            PrivateKey privateKey = SignatureUtil.parseECPrivateKey(decodePrivateKey, SignatureUtil.CURVE_NAME);
            transaction.sign(privateKey);

            // 9. 验证交易
            boolean isValid = transaction.verify();
            Assert.isTrue(isValid, "交易验证失败");

            // 10. 添加到待处理交易池
            blockCache.getPackedTransactions().add(transaction);

            log.info("转账交易创建成功: {}", transaction.getTxHash());
            log.info("交易详情: {}", JSON.toJSONString(transaction));

            return transaction.getTxHash();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建转账交易失败", e);
            throw new BusinessException(ResultCodeEnum.ERROR, "创建交易失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取发送方密钥对
     * @param address 地址
     * @return 密钥对
     */
    private SerializableKeyPair getSenderKeyPair(String address) {
        try {
            // 从文件中读取密钥信息
            File file = ResourceUtils.getFile("file/mineInfo.json");
            try (FileReader reader = new FileReader(file)) {
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = reader.read()) != -1) {
                    sb.append((char) ch);
                }
                
                Map<String, SerializableKeyPair> keyPairMap = JSON.parseObject(sb.toString(),
                        new TypeReference<Map<String, SerializableKeyPair>>() {}.getType());
                return JSON.parseObject(JSON.toJSONString(keyPairMap.get(address)), SerializableKeyPair.class);
            }
        } catch (Exception e) {
            log.error("获取密钥对失败", e);
            return null;
        }
    }
}
