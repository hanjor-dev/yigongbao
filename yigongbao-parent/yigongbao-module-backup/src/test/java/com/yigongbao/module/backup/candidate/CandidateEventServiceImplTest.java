package com.yigongbao.module.backup.candidate;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import com.yigongbao.module.backup.candidate.mapper.OrderBackupCandidateMapper;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;
import com.yigongbao.module.backup.candidate.service.impl.CandidateEventServiceImpl;
import com.yigongbao.module.order.service.OrderMainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CandidateEventServiceImplTest {

    private final OrderBackupCandidateMapper mapper = mock(OrderBackupCandidateMapper.class);
    private final OrderMainService orderService = mock(OrderMainService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<OrderMainService> provider = mock(ObjectProvider.class);
    private final CandidateEventServiceImpl service = new CandidateEventServiceImpl(mapper, provider);

    @Test
    void formalEventIsIdempotentAndReformalIncrementsGeneration() {
        when(provider.getIfAvailable()).thenReturn(orderService);
        OrderMainEntity order = formalOrder(10L);
        when(orderService.getById(10L)).thenReturn(order);
        when(mapper.selectMaxEventId()).thenReturn(0L, 1L);
        when(mapper.selectMaxGeneration(10L)).thenReturn(0, 1);
        when(mapper.selectLatestByOrder(10L)).thenReturn(null,
                candidate(10L, 1, "ACTIVE"), candidate(10L, 1, "CANCELLED"));

        service.recordEligibilityChange(10L, "FORMAL", LocalDateTime.of(2026, 10, 1, 0, 0));
        service.recordEligibilityChange(10L, "FORMAL", LocalDateTime.of(2026, 10, 1, 0, 1));
        service.recordEligibilityChange(10L, "CANCELLED", LocalDateTime.of(2026, 10, 1, 0, 2));
        order.setStatus(2030);
        service.recordEligibilityChange(10L, "FORMAL", LocalDateTime.of(2026, 10, 1, 0, 3));

        verify(mapper, times(2)).insert(any(OrderBackupCandidateEntity.class));
        verify(mapper).updateActiveStatus(10L, "CANCELLED");
        var first = (OrderBackupCandidateEntity) mockingDetails(mapper).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("insert"))
                .findFirst().orElseThrow().getArguments()[0];
        assertThat(((OrderBackupCandidateEntity) first).getGeneration()).isEqualTo(1);
    }

    @Test
    void cursorSearchKeepsHighWatermark() {
        when(mapper.selectHighWatermark()).thenReturn(candidate(10L, 1, "ACTIVE"));
        when(mapper.search(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(candidate(10L, 1, "ACTIVE")));

        var page = service.search(new CandidateSearchQuery("", 10, "", null, null));

        assertThat(page.items()).hasSize(1);
        assertThat(page.highWatermark()).isNotBlank();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void invalidStateIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.recordEligibilityChange(1L, "DRAFT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OrderMainEntity formalOrder(Long id) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setStatus(2030);
        order.setIsDeleted(0);
        order.setCreateTime(LocalDateTime.of(2026, 9, 1, 0, 0));
        return order;
    }

    private static OrderBackupCandidateEntity candidate(Long orderId, int generation, String status) {
        OrderBackupCandidateEntity candidate = new OrderBackupCandidateEntity();
        candidate.setOrderId(orderId);
        candidate.setGeneration(generation);
        candidate.setEventId((long) generation);
        candidate.setEligibleAt(LocalDateTime.of(2026, 10, 1, 0, generation));
        candidate.setEventStatus(status);
        return candidate;
    }
}
