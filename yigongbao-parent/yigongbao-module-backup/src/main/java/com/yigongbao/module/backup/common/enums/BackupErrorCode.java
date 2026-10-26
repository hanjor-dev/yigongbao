package com.yigongbao.module.backup.common.enums;

import org.springframework.http.HttpStatus;

/**
 * 备份内部 API 稳定错误码。
 *
 * <p>枚举名称和 {@link #getCode()} 必须与冻结的 backup-internal-v1 OpenAPI 契约一致。</p>
 */
public enum BackupErrorCode {

    CURSOR_INVALID(HttpStatus.BAD_REQUEST, "分页游标无效", false),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "幂等键已被其他请求使用", false),
    ORDER_NOT_ELIGIBLE(HttpStatus.CONFLICT, "订单不符合备份条件", false),
    RELATION_BROKEN(HttpStatus.CONFLICT, "订单文件关系不完整", false),
    MANIFEST_CHANGED(HttpStatus.PRECONDITION_FAILED, "备份清单已变化", true),
    STATE_CONFLICT(HttpStatus.CONFLICT, "资源状态冲突", true),
    VERIFICATION_REJECTED(HttpStatus.UNAUTHORIZED, "内部请求尚未通过机器身份验证", false),
    ORDER_MISSING(HttpStatus.NOT_FOUND, "订单不存在", false),
    RESTORE_CONFLICT(HttpStatus.CONFLICT, "恢复请求与线上状态冲突", false),
    PLAN_CHANGED(HttpStatus.PRECONDITION_FAILED, "恢复计划已变化", true),
    OBJECT_EVIDENCE_INVALID(HttpStatus.BAD_REQUEST, "对象证据无效", false),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "恢复会话不存在", false),
    CLEANUP_DISABLED(HttpStatus.FORBIDDEN, "COS 实际删除未启用", false),
    LOCAL_COPY_UNSAFE(HttpStatus.PRECONDITION_FAILED, "本地副本未达到安全条件", false),
    SAFETY_EVENT_PENDING(HttpStatus.PRECONDITION_FAILED, "存在待处理安全事件", true),
    VERSIONING_REQUIRED(HttpStatus.PRECONDITION_FAILED, "对象存储版本控制未满足要求", false),
    REFERENCE_INDEX_UNREADY(HttpStatus.PRECONDITION_FAILED, "对象引用索引尚未就绪", true),
    DELETE_PERMIT_REQUIRED(HttpStatus.FORBIDDEN, "缺少有效删除许可", false),
    CLASSIC_CASE_BLOCKED(HttpStatus.PRECONDITION_FAILED, "经典案例保护阻止清理", false),
    REMOTE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "远端服务响应超时", true),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁", true),
    STORAGE_TEMPORARY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "存储暂时不可用", true),
    REMOTE_COMMIT_UNKNOWN(HttpStatus.SERVICE_UNAVAILABLE, "远端提交结果未知", true);

    private final HttpStatus httpStatus;
    private final String message;
    private final boolean retryable;

    BackupErrorCode(HttpStatus httpStatus, String message, boolean retryable) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.retryable = retryable;
    }

    /** 返回契约中的稳定字符串错误码。 */
    public String getCode() {
        return name();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
