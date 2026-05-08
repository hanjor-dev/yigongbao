package com.yigongbao.common.result;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.yigongbao.common.enums.ErrorCodeEnum;
import lombok.Data;

/**
 * 统一返回结果封装类
 * 用于封装 API 的响应数据，提供统一的格式
 *
 * @param <T> 响应数据的类型
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class Result<T> {

    /**
     * 状态码，200 表示成功，其他表示失败
     */
    private Integer code;

    /**
     * 提示信息，成功时返回"操作成功"，失败时返回错误描述
     */
    private String message;

    /**
     * 响应数据，可以是任意类型
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 错误优先级（1=最高优先级，5=最低优先级）
     */
    private Integer priority;

    /**
     * 私有构造函数，使用静态方法创建实例
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私有构造函数，带数据
     *
     * @param data 响应数据
     */
    private Result(T data) {
        this.code = 200;
        this.message = "操作成功";
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私有构造函数，带状态码和消息
     *
     * @param code    状态码
     * @param message 提示信息
     */
    private Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私有构造函数，带状态码、消息和数据
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    响应数据
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应，无返回数据
     *
     * @return Result 实例
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 成功响应，带返回数据
     *
     * @param data 返回数据
     * @return Result 实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    /**
     * 成功响应，自定义消息和返回数据
     *
     * @param message 自定义消息
     * @param data    返回数据
     * @return Result 实例
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     *
     * @param message 错误信息
     * @return Result 实例
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败响应，自定义状态码和错误信息
     *
     * @param code    状态码
     * @param message 错误信息
     * @return Result 实例
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>(code, message);
        result.setPriority(null);
        return result;
    }

    /**
     * 失败响应，使用错误码枚举
     *
     * @param errorCode 错误码枚举
     * @return Result 实例
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode) {
        Result<T> result = new Result<>(errorCode.getCode(), errorCode.getMessage());
        result.setPriority(errorCode.getPriority());
        return result;
    }

    /**
     * 失败响应，使用错误码枚举的 code 和 priority，但自定义错误信息
     *
     * @param errorCode     错误码枚举
     * @param customMessage 自定义错误信息
     * @return Result 实例
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode, String customMessage) {
        Result<T> result = new Result<>(errorCode.getCode(), customMessage);
        result.setPriority(errorCode.getPriority());
        return result;
    }

}
