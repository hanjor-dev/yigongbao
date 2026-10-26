package com.yigongbao.module.backup.candidate.service.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import com.yigongbao.module.backup.candidate.mapper.OrderBackupCandidateMapper;
import com.yigongbao.module.backup.candidate.mapper.OrderBackupOrderMapper;
import com.yigongbao.module.backup.candidate.model.CandidateCursor;
import com.yigongbao.module.backup.candidate.model.CandidateEventStatus;
import com.yigongbao.module.backup.candidate.model.CandidatePage;
import com.yigongbao.module.backup.candidate.model.CandidateReconcileResult;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;
import com.yigongbao.module.backup.candidate.service.CandidateEventService;
import com.yigongbao.module.order.service.OrderMainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 候选事实实现。
 *
 * <p>订单资格事件是可靠事实而不是控制器通知：正式事件只在当前代不存在 ACTIVE
 * 候选时追加，取消/删除只关闭当前 ACTIVE 候选。重复投递因此不会产生第二代，
 * 只有取消后再次正式才会使 generation 加一。</p>
 */
@Service
@Slf4j
public class CandidateEventServiceImpl implements CandidateEventService {

    private final OrderBackupCandidateMapper candidateMapper;
    private final OrderBackupOrderMapper orderMapper;
    private final ObjectProvider<OrderMainService> orderServices;

    public CandidateEventServiceImpl(OrderBackupCandidateMapper candidateMapper,
                                    OrderBackupOrderMapper orderMapper,
                                    ObjectProvider<OrderMainService> orderServices) {
        this.candidateMapper = candidateMapper;
        this.orderMapper = orderMapper;
        this.orderServices = orderServices;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEligibilityChange(Long orderId, String targetState) {
        recordEligibilityChange(orderId, targetState, LocalDateTime.now());
    }

    /** 保留事件发生时间，供 BEFORE_COMMIT 监听器写入稳定分页游标。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEligibilityChange(Long orderId, String targetState, LocalDateTime occurredAt) {
        requireState(targetState);
        if (orderId == null || occurredAt == null) {
            throw new IllegalArgumentException("订单资格事件参数不能为空");
        }

        OrderMainEntity order = findOrder(orderId);
        if (isFormalState(targetState) && order != null
                && !OrderEligibility.isEligible(order)) {
            log.info("忽略非正式订单备份候选事件: orderId={}, status={}, isDeleted={}",
                    orderId, order.getStatus(), order.getIsDeleted());
            return;
        }

        if (!isFormalState(targetState)) {
            String terminalState = OrderEligibilityChangedEvent.DELETED.equals(targetState)
                    ? CandidateEventStatus.DELETED.name() : CandidateEventStatus.CANCELLED.name();
            int changed = candidateMapper.updateActiveStatus(orderId, terminalState);
            log.info("关闭订单备份候选: orderId={}, targetState={}, candidateState={}, changed={}",
                    orderId, targetState, terminalState, changed);
            return;
        }

        OrderBackupCandidateEntity latest = candidateMapper.selectLatestByOrder(orderId);
        if (latest != null && CandidateEventStatus.ACTIVE.name().equals(latest.getEventStatus())) {
            log.debug("重复订单备份候选事件已幂等忽略: orderId={}, generation={}",
                    orderId, latest.getGeneration());
            return;
        }

        int generation = nextGeneration(orderId);
        OrderBackupCandidateEntity candidate = new OrderBackupCandidateEntity();
        candidate.setEventId(nextEventId());
        candidate.setOrderId(orderId);
        candidate.setGeneration(generation);
        candidate.setEligibleAt(occurredAt);
        candidate.setOrderCreatedAt(order == null ? null : order.getCreateTime());
        candidate.setEventStatus(CandidateEventStatus.ACTIVE.name());
        try {
            candidateMapper.insert(candidate);
        } catch (DuplicateKeyException duplicate) {
            // 并发或消息重投只能落一个 (order_id,generation)，再次读取即为幂等成功。
            OrderBackupCandidateEntity existing = candidateMapper.selectByOrderGeneration(orderId, generation);
            if (existing == null) {
                throw duplicate;
            }
            log.debug("并发订单备份候选事件已幂等收敛: orderId={}, generation={}", orderId, generation);
            return;
        }
        log.info("记录订单备份候选: orderId={}, generation={}, eventId={}",
                orderId, generation, candidate.getEventId());
    }

    @Override
    @Transactional(readOnly = true)
    public CandidatePage search(CandidateSearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("候选查询条件不能为空");
        }
        CandidateCursor cursor = CandidateCursor.decode(query.cursor());
        CandidateCursor highWatermark = CandidateCursor.decode(query.highWatermark());
        if (cursor != null && highWatermark == null) {
            throw new IllegalArgumentException("后续页必须携带 highWatermark");
        }
        if (highWatermark == null) {
            OrderBackupCandidateEntity high = candidateMapper.selectHighWatermark();
            highWatermark = high == null ? null : toCursor(high);
        }

        List<OrderBackupCandidateEntity> queried = candidateMapper.search(
                query.eligibleFrom(), query.eligibleTo(),
                cursor == null ? null : cursor.eligibleAt(),
                cursor == null ? null : cursor.eventId(),
                highWatermark == null ? null : highWatermark.eligibleAt(),
                highWatermark == null ? null : highWatermark.eventId(),
                query.pageSize() + 1);
        boolean hasMore = queried.size() > query.pageSize();
        List<OrderBackupCandidateEntity> items = hasMore
                ? new ArrayList<>(queried.subList(0, query.pageSize()))
                : queried;
        String nextCursor = hasMore && !items.isEmpty() ? toCursor(items.get(items.size() - 1)).encode() : "";
        return new CandidatePage(items, nextCursor, hasMore,
                highWatermark == null ? "" : highWatermark.encode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CandidateReconcileResult reconcile(Long orderIdFrom, Long orderIdTo) {
        if (orderIdFrom == null || orderIdTo == null || orderIdFrom > orderIdTo) {
            throw new IllegalArgumentException("订单范围无效");
        }
        // 必须使用显式含逻辑删除的查询，否则已删除订单永远无法关闭 ACTIVE 候选。
        List<OrderMainEntity> orders = orderMapper.selectRangeIncludingDeleted(orderIdFrom, orderIdTo);
        int repaired = 0;
        int cancelled = 0;
        int deleted = 0;
        for (OrderMainEntity order : orders) {
            boolean eligible = OrderEligibility.isEligible(order);
            OrderBackupCandidateEntity latest = candidateMapper.selectLatestByOrder(order.getId());
            if (eligible) {
                if (latest == null || !CandidateEventStatus.ACTIVE.name().equals(latest.getEventStatus())) {
                    recordEligibilityChange(order.getId(), OrderEligibility.FORMAL.value,
                            order.getCreateTime() == null ? LocalDateTime.now() : order.getCreateTime());
                    repaired++;
                }
                continue;
            }
            String terminal = order.getIsDeleted() != null && StatusConstants.YES == order.getIsDeleted()
                    ? CandidateEventStatus.DELETED.name() : CandidateEventStatus.CANCELLED.name();
            if (latest != null && CandidateEventStatus.ACTIVE.name().equals(latest.getEventStatus())) {
                candidateMapper.updateActiveStatus(order.getId(), terminal);
                if (CandidateEventStatus.DELETED.name().equals(terminal)) {
                    deleted++;
                } else {
                    cancelled++;
                }
            }
        }
        return new CandidateReconcileResult(orders.size(), repaired, cancelled, deleted);
    }

    private OrderMainEntity findOrder(Long orderId) {
        OrderMainService service = orderServices.getIfAvailable();
        return service == null ? null : service.getById(orderId);
    }

    private int nextGeneration(Long orderId) {
        Integer max = candidateMapper.selectMaxGeneration(orderId);
        return (max == null ? 0 : max) + 1;
    }

    private long nextEventId() {
        if (candidateMapper.allocateEventId() != 1) {
            throw new IllegalStateException("备份候选事件号分配器不可用");
        }
        Long allocated = candidateMapper.selectAllocatedEventId();
        if (allocated == null || allocated <= 0) {
            throw new IllegalStateException("备份候选事件号分配失败");
        }
        return allocated;
    }

    private CandidateCursor toCursor(OrderBackupCandidateEntity candidate) {
        return new CandidateCursor(candidate.getEligibleAt(), candidate.getEventId());
    }

    private void requireState(String targetState) {
        if (!isFormalState(targetState)
                && !CandidateEventStatus.CANCELLED.name().equals(targetState)
                && !CandidateEventStatus.DELETED.name().equals(targetState)
                && !OrderEligibilityChangedEvent.DRAFT.equals(targetState)) {
            throw new IllegalArgumentException("不支持的订单资格状态: " + targetState);
        }
    }

    private boolean isFormalState(String targetState) {
        return OrderEligibility.FORMAL.value.equals(targetState)
                || OrderEligibilityChangedEvent.ACTIVE.equals(targetState);
    }

    private enum OrderEligibility {
        FORMAL("FORMAL");

        private final String value;

        OrderEligibility(String value) {
            this.value = value;
        }

        private static boolean isEligible(OrderMainEntity order) {
            return order != null
                    && (order.getIsDeleted() == null || order.getIsDeleted() == 0)
                    && !FlowStatusEnum.DRAFT.getValue().equals(order.getStatus())
                    && !FlowStatusEnum.CANCELLED.getValue().equals(order.getStatus());
        }
    }
}
