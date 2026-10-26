package com.yigongbao.module.backup.candidate.controller;

import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import com.yigongbao.module.backup.candidate.model.CandidatePage;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;
import com.yigongbao.module.backup.candidate.service.CandidateEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupCandidateControllerTest {

    @Test
    void queryConvertsNonUtcOffsetByInstantAndResponseKeepsRfc3339Offset() {
        CandidateEventService service = mock(CandidateEventService.class);
        OrderBackupCandidateEntity entity = new OrderBackupCandidateEntity();
        entity.setEligibleAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        entity.setOrderCreatedAt(LocalDateTime.of(2026, 9, 30, 0, 0));
        entity.setEventId(1L);
        entity.setOrderId(2L);
        entity.setGeneration(1);
        entity.setEventStatus("ACTIVE");
        when(service.search(any())).thenReturn(new CandidatePage(List.of(entity), "", false, ""));
        BackupCandidateController controller = new BackupCandidateController(service);

        BackupCandidateController.CandidateSearchRequest request =
                new BackupCandidateController.CandidateSearchRequest();
        request.pageSize = 10;
        request.eligibleFrom = "2026-10-01T08:00:00+08:00";
        var response = controller.search(request, mock(HttpServletRequest.class));

        var captor = org.mockito.ArgumentCaptor.forClass(CandidateSearchQuery.class);
        verify(service).search(captor.capture());
        assertThat(captor.getValue().eligibleFrom()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        var item = response.getBody().data().items().get(0);
        assertThat(item.eligibleAt()).isEqualTo(OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }
}
