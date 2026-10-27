package com.yigongbao.module.backup.common;

import com.yigongbao.module.backup.common.enums.BackupErrorCode;
import com.yigongbao.module.backup.common.response.InternalApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiResponseTest {

    @Test
    void errorCodesMatchFrozenOpenApiContract() {
        Set<String> frozenCodes = Set.of(
                "CURSOR_INVALID", "IDEMPOTENCY_KEY_REUSED", "ORDER_NOT_ELIGIBLE",
                "RELATION_BROKEN", "MANIFEST_CHANGED", "STATE_CONFLICT",
                "VERIFICATION_REJECTED", "ORDER_MISSING", "RESTORE_CONFLICT",
                "PLAN_CHANGED", "OBJECT_EVIDENCE_INVALID", "SESSION_NOT_FOUND",
                "CLEANUP_DISABLED", "LOCAL_COPY_UNSAFE", "SAFETY_EVENT_PENDING",
                "VERSIONING_REQUIRED", "REFERENCE_INDEX_UNREADY", "DELETE_PERMIT_REQUIRED",
                "CLASSIC_CASE_BLOCKED", "REMOTE_TIMEOUT", "RATE_LIMITED",
                "STORAGE_TEMPORARY_UNAVAILABLE", "REMOTE_COMMIT_UNKNOWN"
        );

        assertThat(BackupErrorCode.values())
                .extracting(BackupErrorCode::getCode)
                .containsExactlyInAnyOrderElementsOf(frozenCodes);
    }

    @Test
    void errorEnvelopeKeepsStableMachineReadableFields() {
        InternalApiResponse<Void> response = InternalApiResponse.error(
                BackupErrorCode.VERIFICATION_REJECTED, "request-1");

        assertThat(response.code()).isEqualTo("VERIFICATION_REJECTED");
        assertThat(response.message()).isNotBlank();
        assertThat(response.requestId()).isEqualTo("request-1");
        assertThat(response.data()).isNull();
        assertThat(response.retryable()).isFalse();
        assertThat(response.retryAfterSeconds()).isNull();
        assertThat(response.details()).isEmpty();
    }
}
