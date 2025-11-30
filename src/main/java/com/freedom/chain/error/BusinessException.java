package com.freedom.chain.error;

import com.freedom.chain.enumst.ResultCode;
import lombok.Getter;

/**
 * @description: 业务异常类
 * @author: freedom
 * @create: 2025-11-28
 **/
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * 错误消息
     */
    private final String msg;
    
    /**
     * 使用ResultCode构造异常
     * @param resultCode 结果码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }
    
    /**
     * 使用ResultCode和自定义消息构造异常
     * @param resultCode 结果码枚举
     * @param msg 自定义错误消息
     */
    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = msg;
    }
    
    /**
     * 使用错误码和消息构造异常
     * @param code 错误码
     * @param msg 错误消息
     */
    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
    
    /**
     * 使用ResultCode、消息和原因构造异常
     * @param resultCode 结果码枚举
     * @param msg 错误消息
     * @param cause 异常原因
     */
    public BusinessException(ResultCode resultCode, String msg, Throwable cause) {
        super(msg, cause);
        this.code = resultCode.getCode();
        this.msg = msg;
    }
}
