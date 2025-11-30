package com.freedom.chain.model.ledger;

import lombok.Data;

import java.math.BigInteger;

/**
 * @description: 交易_输出内部类（UTXO模型核心）
 * @author: freedom
 * @date: 2025/11/21 23:47
 **/
@Data
public class UtxoOutput {
    /**
     * 接收方地址
     */
    private String recipientAddress; 
    /**
     * 输出金额（最小单位）
     */
    private BigInteger amount; 
    /**
     * 锁定脚本（限制解锁条件）
     */
    private String lockScript; 
    /**
     * 输出索引
     */
    private int outputIndex; 
    /**
     * 是否已被花费
     */
    private boolean isSpent = false;
}