package com.freedom.chain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description: 转账请求DTO
 * @author: freedom
 * @create: 2025-11-28
 **/
@Data
public class TransferRequest {
    
    /**
     * 发送方地址
     */
    private String fromAddress;
    
    /**
     * 接收方地址
     */
    private String toAddress;
    
    /**
     * 转账金额
     */
    private BigDecimal amount;
    
    /**
     * 手续费
     */
    private BigDecimal fee;
}
