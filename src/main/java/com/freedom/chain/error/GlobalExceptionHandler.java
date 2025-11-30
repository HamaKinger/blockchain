package com.freedom.chain.error;

import com.freedom.chain.enumst.ResultCodeEnum;
import com.freedom.chain.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * @description: 全局异常处理器
 * @author: freedom
 * @create: 2025-11-28
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：code={}, msg={}", e.getCode(), e.getMsg(), e);
        return Result.failed(ResultCodeEnum.getByCode(e.getCode()), e.getMsg());
    }

    /**
     * 处理参数校验异常 (RequestBody方式)
     * @param e 方法参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常：{}", errorMsg, e);
        return Result.failed(ResultCodeEnum.FAILED, "参数校验失败：" + errorMsg);
    }

    /**
     * 处理参数绑定异常 (Form方式)
     * @param e 参数绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数绑定异常：{}", errorMsg, e);
        return Result.failed(ResultCodeEnum.FAILED, "参数绑定失败：" + errorMsg);
    }

    /**
     * 处理非法参数异常
     * @param e 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常：{}", e.getMessage(), e);
        return Result.failed(ResultCodeEnum.FAILED, e.getMessage());
    }

    /**
     * 处理空指针异常
     * @param e 空指针异常
     * @return 统一响应结果
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.failed(ResultCodeEnum.ERROR, "系统内部错误：空指针异常");
    }

    /**
     * 处理运行时异常
     * @param e 运行时异常
     * @return 统一响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常：{}", e.getMessage(), e);
        return Result.failed(ResultCodeEnum.ERROR, "系统运行异常：" + e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     * @param e 异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.failed(ResultCodeEnum.UNKNO, "系统未知异常，请联系管理员");
    }
}
