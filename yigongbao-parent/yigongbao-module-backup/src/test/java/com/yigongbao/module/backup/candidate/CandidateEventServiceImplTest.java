package com.yigongbao.module.backup.candidate;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import com.yigongbao.module.backup.candidate.mapper.OrderBackupCandidateMapper;
import com.yigongbao.module.backup.candidate.mapper.OrderBackupOrderMapper;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;
import com.yigongbao.module.backup.candidate.service.impl.CandidateEventServiceImpl;
import com.yigongbao.module.order.service.OrderMainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CandidateEventServiceImplTest {

    private final OrderBackupCandidateMapper mapper = mock(OrderBackupCandidateMapper.class);
    private final OrderBackupOrderMapper orderMapper = mock(OrderBackupOrderMapper.class);
    private final OrderMainService orderService = mock(OrderMainService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<OrderMainService> provider = mock(ObjectProvider.class);
    private final CandidateEventServiceImpl service = new CandidateEventServiceImpl(mapper, orderMapper, provider);

    @Test
    void formalEventIsIdempotentAndReformalIncrementsGeneration() {
        when(provider.getIfAvailable()).thenReturn(orderService);
        OrderMainEntity order = formalOrder(10L);
        when(orderService.getById(10L)).thenReturn(order);
        when(mapper.allocateEventId()).thenReturn(1);
        when(mapper.selectAllocatedEventId()).thenReturn(1L, 2L);
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
    void withdrawingToDraftClosesActiveCandidateAsCancelled() {
        when(provider.getIfAvailable()).thenReturn(orderService);
        when(orderService.getById(11L)).thenReturn(formalOrder(11L));
        when(mapper.allocateEventId()).thenReturn(1);
        when(mapper.selectAllocatedEventId()).thenReturn(1L);

        service.recordEligibilityChange(11L, "FORMAL", LocalDateTime.now());
        service.recordEligibilityChange(11L, "DRAFT", LocalDateTime.now());

        verify(mapper).updateActiveStatus(11L, "CANCELLED");
    }

    @Test
    void reconcileUsesDeletedRowsAndClosesActiveCandidate() {
        OrderMainEntity deleted = formalOrder(12L);
        deleted.setIsDeleted(1);
        when(orderMapper.selectRangeIncludingDeleted(12L, 12L)).thenReturn(List.of(deleted));
        when(mapper.selectLatestByOrder(12L)).thenReturn(candidate(12L, 1, "ACTIVE"));

        var result = service.reconcile(12L, 12L);

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.deleted()).isEqualTo(1);
        verify(mapper).updateActiveStatus(12L, "DELETED");
    }

    @Test
    void databaseAllocatorContractKeepsEventIdsUniqueAcrossConcurrentInstances() throws Exception {
        when(provider.getIfAvailable()).thenReturn(orderService);
        when(orderService.getById(anyLong())).thenAnswer(invocation -> formalOrder(invocation.getArgument(0)));
        when(mapper.selectLatestByOrder(anyLong())).thenReturn(null);
        when(mapper.selectMaxGeneration(anyLong())).thenReturn(0);
        AtomicLong sequence = new AtomicLong();
        ThreadLocal<Long> lastAllocated = new ThreadLocal<>();
        when(mapper.allocateEventId()).thenAnswer(invocation -> {
            lastAllocated.set(sequence.incrementAndGet());
            return 1;
        });
        when(mapper.selectAllocatedEventId()).thenAnswer(invocation -> lastAllocated.get());
        List<OrderBackupCandidateEntity> inserted = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            inserted.add(invocation.getArgument(0));
            return 1;
        }).when(mapper).insert(any(OrderBackupCandidateEntity.class));

        int count = 16;
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        var futures = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> executor.submit(() -> {
                    start.await();
                    new CandidateEventServiceImpl(mapper, orderMapper, provider)
                            .recordEligibilityChange(100L + index, "FORMAL", LocalDateTime.now());
                    return null;
                })).toList();
        start.countDown();
        for (var future : futures) {
            future.get();
        }
        executor.shutdownNow();

        assertThat(inserted).hasSize(count);
        assertThat(inserted.stream().map(OrderBackupCandidateEntity::getEventId).distinct()).hasSize(count);
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
                () -> service.recordEligibilityChange(1L, "NOT_A_STATE"))
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
