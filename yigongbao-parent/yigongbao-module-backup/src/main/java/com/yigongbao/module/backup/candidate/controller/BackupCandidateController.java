package com.yigongbao.module.backup.candidate.controller;

import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import com.yigongbao.module.backup.candidate.model.CandidatePage;
import com.yigongbao.module.backup.candidate.model.CandidateReconcileResult;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;
import com.yigongbao.module.backup.candidate.service.CandidateEventService;
import com.yigongbao.module.backup.common.enums.BackupErrorCode;
import com.yigongbao.module.backup.common.response.InternalApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/** 供本地备份系统发现候选和执行漏单对账的内部接口。 */
@RestController
@RequestMapping("/internal/backup/v1/candidates")
public class BackupCandidateController {

    private final CandidateEventService candidateEventService;

    public BackupCandidateController(CandidateEventService candidateEventService) {
        this.candidateEventService = candidateEventService;
    }

    @PostMapping("/search")
    public ResponseEntity<InternalApiResponse<CandidatePageData>> search(
            @RequestBody CandidateSearchRequest request, HttpServletRequest servletRequest) {
        String requestId = requestId(servletRequest);
        try {
            CandidatePage page = candidateEventService.search(new CandidateSearchQuery(
                    request.cursor == null ? "" : request.cursor,
                    request.pageSize == null ? 100 : request.pageSize,
                    request.highWatermark == null ? "" : request.highWatermark,
                    parseTime(request.eligibleFrom), parseTime(request.eligibleTo)));
            CandidatePageData data = new CandidatePageData(page.items().stream()
                    .map(CandidateItem::from).toList(), page.nextCursor(), page.hasMore(), page.highWatermark());
            return ResponseEntity.ok(InternalApiResponse.success(data, requestId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(BackupErrorCode.CURSOR_INVALID.getHttpStatus())
                    .body(InternalApiResponse.error(BackupErrorCode.CURSOR_INVALID, requestId));
        }
    }

    @PostMapping("/reconcile")
    public ResponseEntity<InternalApiResponse<CandidateReconcileResult>> reconcile(
            @RequestBody CandidateReconcileRequest request, HttpServletRequest servletRequest) {
        String requestId = requestId(servletRequest);
        try {
            CandidateReconcileResult result = candidateEventService.reconcile(
                    request.orderIdFrom, request.orderIdTo);
            return ResponseEntity.ok(InternalApiResponse.success(result, requestId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(BackupErrorCode.CURSOR_INVALID.getHttpStatus())
                    .body(InternalApiResponse.error(BackupErrorCode.CURSOR_INVALID, requestId));
        }
    }

    private LocalDateTime parseTime(String value) {
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("backup.security.requestId");
        return requestId == null ? "backup-candidate" : requestId.toString();
    }

    /** 请求 DTO 使用字符串时间，严格对应契约中的 RFC3339。 */
    public static class CandidateSearchRequest {
        public String cursor;
        public Integer pageSize;
        public String highWatermark;
        public String eligibleFrom;
        public String eligibleTo;
    }

    public static class CandidateReconcileRequest {
        public Long orderIdFrom;
        public Long orderIdTo;
    }

    public record CandidatePageData(List<CandidateItem> items, String nextCursor,
                                    boolean hasMore, String highWatermark) {
    }

    public record CandidateItem(Long eventId, Long orderId, Integer generation,
                                 LocalDateTime eligibleAt, LocalDateTime orderCreatedAt,
                                 String eventStatus) {
        static CandidateItem from(OrderBackupCandidateEntity entity) {
            return new CandidateItem(entity.getEventId(), entity.getOrderId(), entity.getGeneration(),
                    entity.getEligibleAt(), entity.getOrderCreatedAt(), entity.getEventStatus());
        }
    }
}
