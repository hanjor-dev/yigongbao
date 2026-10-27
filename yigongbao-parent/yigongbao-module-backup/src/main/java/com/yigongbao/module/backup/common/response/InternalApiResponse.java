package com.yigongbao.module.backup.common.response;

import com.yigongbao.module.backup.common.enums.BackupErrorCode;

import java.util.List;

/**
 * 内部 API 稳定响应信封。
 *
 * @param code 响应码，成功固定为 OK，失败使用 {@link BackupErrorCode}
 * @param message 可读消息
 * @param requestId 请求追踪标识
 * @param data 业务数据，错误响应固定为 null
 * @param retryable 客户端是否可重试
 * @param retryAfterSeconds 建议重试等待秒数，无建议时为 null
 * @param details 结构化错误详情
 * @param <T> 业务数据类型
 */
public record InternalApiResponse<T>(
        String code,
        String message,
        String requestId,
        T data,
        boolean retryable,
        Integer retryAfterSeconds,
        List<ErrorDetail> details
) {

    public InternalApiResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    /** 构造成功响应。 */
    public static <T> InternalApiResponse<T> success(T data, String requestId) {
        return new InternalApiResponse<>("OK", "操作成功", requestId, data, false, null, List.of());
    }

    /** 构造无重试等待时间的错误响应。 */
    public static <T> InternalApiResponse<T> error(BackupErrorCode errorCode, String requestId) {
        return error(errorCode, requestId, null, List.of());
    }

    /** 构造完整错误响应。 */
    public static <T> InternalApiResponse<T> error(
            BackupErrorCode errorCode,
            String requestId,
            Integer retryAfterSeconds,
            List<ErrorDetail> details
    ) {
        return new InternalApiResponse<>(errorCode.getCode(), errorCode.getMessage(), requestId,
                null, errorCode.isRetryable(), retryAfterSeconds, details);
    }

    /** 内部 API 字段级错误详情。 */
    public record ErrorDetail(String field, String reason) {
    }
}
