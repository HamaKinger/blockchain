package com.freedom.chain.error;

import com.freedom.chain.enumst.ResultCodeEnum;

/**
 * @description: 断言工具类 - 用于参数校验和业务逻辑判断
 * @author: freedom
 * @create: 2025-11-28
 **/
public class Assert {

    /**
     * 断言对象不为空，为空则抛出异常
     * @param object 待检查对象
     * @param message 错误消息
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言字符串不为空，为空则抛出异常
     * @param str 待检查字符串
     * @param message 错误消息
     */
    public static void notEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言表达式为真，为假则抛出异常
     * @param expression 表达式
     * @param message 错误消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言表达式为假，为真则抛出异常
     * @param expression 表达式
     * @param message 错误消息
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言两个对象相等，不相等则抛出异常
     * @param obj1 对象1
     * @param obj2 对象2
     * @param message 错误消息
     */
    public static void equals(Object obj1, Object obj2, String message) {
        if (obj1 == null && obj2 == null) {
            return;
        }
        if (obj1 == null || !obj1.equals(obj2)) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言数字大于0，否则抛出异常
     * @param number 数字
     * @param message 错误消息
     */
    public static void greaterThanZero(Number number, String message) {
        if (number == null || number.doubleValue() <= 0) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 断言数字大于等于0，否则抛出异常
     * @param number 数字
     * @param message 错误消息
     */
    public static void notNegative(Number number, String message) {
        if (number == null || number.doubleValue() < 0) {
            throw new BusinessException(ResultCodeEnum.FAILED, message);
        }
    }

    /**
     * 直接抛出业务异常
     * @param message 错误消息
     */
    public static void fail(String message) {
        throw new BusinessException(ResultCodeEnum.FAILED, message);
    }

    /**
     * 使用指定错误码抛出业务异常
     * @param resultCode 错误码
     * @param message 错误消息
     */
    public static void fail(com.freedom.chain.enumst.ResultCode resultCode, String message) {
        throw new BusinessException(resultCode, message);
    }
}
