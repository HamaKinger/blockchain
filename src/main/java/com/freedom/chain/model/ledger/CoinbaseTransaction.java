package com.freedom.chain.model.ledger;

import com.freedom.chain.utils.AddressUtil;
import com.freedom.chain.utils.CryptoUtil;
import lombok.Data;

import java.math.BigInteger;
import java.util.Collections;

/**
 * 挖矿奖励交易（Coinbase Transaction）：区块的第一笔交易，无输入，向矿工发放奖励
 */
@Data
public class CoinbaseTransaction extends PublicLedgerTransaction {
    // 区块奖励金额（固定，如比特币初始50 BTC，每4年减半）
    private static final BigInteger BLOCK_REWARD = BigInteger.valueOf(50_00000000L); // 50 BTC（以聪为单位）

    /**
     * 构造方法：直接生成挖矿奖励交易
     * @param minerAddress 矿工地址（接收奖励）
     * @param blockHeight 区块高度（用于生成唯一交易哈希，避免重复）
     */
    public CoinbaseTransaction(String minerAddress, long blockHeight) {
        // 1. 基础字段设置
        super.setFromAddress("0x0000000000000000000000000000000000000000"); // 全零地址（无发起方）
        super.setToAddress(minerAddress); // 接收方=矿工地址
        super.setTimestamp(System.currentTimeMillis());
        super.setChainId(1);
        super.setMemo("Coinbase for block " + blockHeight); // 备注区块高度
        super.setVersion(1);

        // 2. 交易输出（仅一个：矿工接收奖励）
        UtxoOutput rewardOutput = new UtxoOutput();
        rewardOutput.setRecipientAddress(minerAddress);
        rewardOutput.setAmount(BLOCK_REWARD); // 区块奖励金额
        rewardOutput.setOutputIndex(0); // 唯一输出，索引为0
        // 锁定脚本：只有矿工地址的私钥能解锁（标准P2PKH脚本）
        byte[] minerPubKeyHash = AddressUtil.addressToPubKeyHash(minerAddress); // 从矿工地址推导公钥哈希
        String minerPubKeyHashHex = CryptoUtil.bytesToHex(minerPubKeyHash);
        rewardOutput.setLockScript("OP_DUP OP_HASH160 " + minerPubKeyHashHex + " OP_EQUALVERIFY OP_CHECKSIG");

        super.setUtxoOutputs(Collections.singletonList(rewardOutput));
        super.setUtxoInputs(Collections.emptyList()); // Coinbase交易无输入

        // 3. 生成交易哈希（无输入，基于输出、区块高度、时间戳）
        generateTxHash(blockHeight);
    }

    /**
     * 生成Coinbase交易哈希（特殊逻辑：无输入，避免哈希冲突）
     * @param blockHeight 区块高度（唯一标识，确保同一矿工在不同区块的奖励交易哈希不同）
     */
    public void generateTxHash(long blockHeight) {
        String hashSource = super.getFromAddress() + 
                           super.getToAddress() + 
                           super.getTimestamp() + 
                           blockHeight + // 关键：加入区块高度，避免重复哈希
                           super.getUtxoOutputs().toString() + 
                           super.getMemo();
        super.setTxHash(CryptoUtil.sha256(hashSource));
    }

    // 重写父类的verify方法（Coinbase交易无需验证签名和输入，仅验证基础合法性）
    @Override
    public boolean verify() {
        // 1. 验证输出金额是否等于区块奖励
        BigInteger totalOutput = super.getUtxoOutputs().stream()
                .map(UtxoOutput::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        if (!totalOutput.equals(BLOCK_REWARD)) {
            return false;
        }

        // 2. 验证fromAddress是全零地址
        if (!"0x0000000000000000000000000000000000000000".equals(super.getFromAddress())) {
            return false;
        }

        // 3. 验证无输入
        if (super.getUtxoInputs() != null && !super.getUtxoInputs().isEmpty()) {
            return false;
        }

        // 4. 验证交易哈希正确
        String tempTxHash = CryptoUtil.sha256(
                super.getFromAddress() + 
                super.getToAddress() + 
                super.getTimestamp() + 
                (super.getMemo().contains("block ") ? Long.parseLong(super.getMemo().split(" ")[3]) : 0) +
                super.getUtxoOutputs().toString() + 
                super.getMemo()
        );
        if (!tempTxHash.equals(super.getTxHash())) {
            return false;
        }

        return true;
    }
}