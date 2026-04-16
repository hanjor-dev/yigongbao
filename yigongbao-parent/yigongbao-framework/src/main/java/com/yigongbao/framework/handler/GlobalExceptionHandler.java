package com.yigongbao.framework.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
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
        // 获取第一个校验失败的错误信息，避免暴露内部字段名
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
     * 处理方法参数校验异常（Spring 6 新增）
     * 当 Controller 方法参数使用 @Min/@Max/@Size 等校验注解且校验失败时触发
     *
     * @param e 方法参数校验异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String errorMessage = e.getAllErrors().stream()
            .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.toString())
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
        return Result.error(400, "缺少必要的请求参数，请刷新页面后重试");
    }

    /**
     * 处理路径变量缺失异常
     * 当 URL 路径中的占位符（如 {id}）无法匹配时触发
     *
     * @param e 路径变量缺失异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingPathVariableException(MissingPathVariableException e) {
        log.warn("路径变量缺失：参数名={}", e.getVariableName());
        return Result.error(400, "请求的路径参数不完整，请检查URL是否正确");
    }

    /**
     * 处理参数类型不匹配异常
     * 当 URL 参数值无法转换为目标类型时触发，如 id=abc 期望 Long 类型
     *
     * @param e 参数类型不匹配异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        String paramValue = e.getValue() != null ? e.getValue().toString() : "空";
        log.warn("参数类型不匹配：参数名={}, 传入值={}, 期望类型={}", paramName, paramValue, e.getRequiredType());
        return Result.error(400, "请求的参数格式有误，请刷新页面后重试");
    }

    /**
     * 处理 JSON 解析异常（请求体格式错误）
     * 当请求体 JSON 格式不正确时触发，如缺少逗号、引号不匹配等
     *
     * @param e JSON 解析异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMessage();
        String userMessage;
        if (message != null && message.contains("Required request body is missing")) {
            userMessage = "请求参数缺失，请检查是否正确提交了数据";
        } else {
            userMessage = "请求参数格式错误，请检查数据格式后重试";
        }
        log.warn("参数格式错误：{}", message);
        return Result.error(400, userMessage);
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
     * 处理文件大小超出限制异常
     * 由 Spring Multipart 层在请求解析阶段抛出，早于 Controller 执行
     * 对应配置：spring.servlet.multipart.max-file-size / max-request-size
     *
     * @param e 文件大小超出异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超出系统限制：{}", e.getMessage());
        return Result.error(664, "文件大小超出系统允许的最大限制，请压缩后重试");
    }

    /**
     * 处理 Multipart 请求解析异常
     * 当请求不是合法的 multipart 格式，或文件流读取失败时触发
     *
     * @param e Multipart 异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMultipartException(MultipartException e) {
        log.warn("文件上传请求解析失败：{}", e.getMessage());
        return Result.error(661, "文件上传失败，请检查文件格式后重试");
    }

    /**
     * 处理数据库唯一键冲突异常
     * 当插入或更新的数据违反唯一约束时触发，如用户名、编码重复等
     *
     * @param e 数据完整性异常实例
     * @return 统一返回结果
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SQLIntegrityConstraintViolationException sqlEx) {
            String message = sqlEx.getMessage();
            if (message != null && message.contains("Duplicate entry")) {
                log.warn("唯一键冲突：{}", message);
                return Result.error(409, "数据已存在，请勿重复提交");
            }
        }
        log.error("数据完整性异常：", e);
        return Result.error(500, "数据操作失败，请稍后再试");
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
