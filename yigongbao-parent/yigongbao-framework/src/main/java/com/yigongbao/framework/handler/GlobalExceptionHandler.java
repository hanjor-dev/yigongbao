package com.yigongbao.framework.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获并处理项目中的各类异常，返回统一的错误响应
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 BusinessException
     * 用于业务逻辑中主动抛出的异常，如参数校验失败、业务规则不满足等
     *
     * @param e 业务异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Sa-Token 未登录异常
     * 当用户未登录或 token 无效时触发
     *
     * @param e 未登录异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录：{}", e.getMessage());
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }

    /**
     * 处理参数校验异常（@Valid 注解在请求体上）
     * 当 Controller 方法参数使用了 @Valid 注解且校验失败时触发
     *
     * @param e 方法参数校验异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的错误信息
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数校验失败：{}", errorMessage);
        return Result.error(400, errorMessage);
    }

    /**
     * 处理参数校验异常（@Validated 注解在单个参数上）
     * 当 Controller 方法参数使用了 @Validated 且校验失败时触发
     *
     * @param e 参数校验异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数校验失败：{}", errorMessage);
        return Result.error(400, errorMessage);
    }

    /**
     * 处理绑定异常（表单提交时的参数绑定失败）
     *
     * @param e 绑定异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数绑定失败：{}", errorMessage);
        return Result.error(400, errorMessage);
    }

    /**
     * 处理请求参数缺失异常
     *
     * @param e 缺失参数异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return Result.error(400, "缺少参数：" + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     *
     * @param e 参数类型不匹配异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getName());
        return Result.error(400, "参数类型错误：" + e.getName());
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e 请求方法不支持异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMethod());
        return Result.error(405, "不支持的请求方法：" + e.getMethod());
    }

    /**
     * 处理 404 异常
     *
     * @param e 404 异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求路径不存在：{}", e.getRequestURL());
        return Result.error(404, "请求路径不存在：" + e.getRequestURL());
    }

    /**
     * 处理其他未知异常
     * 这是最后的异常处理兜底，避免服务器返回 500 时返回裸露的错误信息
     *
     * @param e 未知异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }

}
