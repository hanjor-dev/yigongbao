package com.yigongbao.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于在业务逻辑中抛出可预期的异常，如参数校验失败、业务规则不满足等
 * 该异常会被全局异常处理器捕获并转换为统一的错误响应
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码，用于标识具体的错误类型
     * 前端可以根据错误码进行不同的处理，如显示不同的提示信息
     */
    private final Integer code;

    /**
     * 错误信息，用于显示给用户的错误描述
     */
    private final String message;

    /**
     * 构造方法，默认错误码为 400（客户端请求错误）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.message = message;
    }

    /**
     * 构造方法，自定义错误码
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法，带异常原因
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
        this.message = message;
    }

    /**
     * 构造方法，自定义错误码和异常原因
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   原始异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

}
