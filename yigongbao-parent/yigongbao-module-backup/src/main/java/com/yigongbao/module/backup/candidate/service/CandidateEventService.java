package com.yigongbao.module.backup.candidate.service;

import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.module.backup.candidate.model.CandidatePage;
import com.yigongbao.module.backup.candidate.model.CandidateReconcileResult;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;

import java.time.LocalDateTime;

/** 订单备份候选可靠事件服务。 */
public interface CandidateEventService {

    /** 在订单状态事务内追加或关闭候选事实，重复事件必须幂等。 */
    void recordEligibilityChange(Long orderId, String targetState);

    /** 保留订单事务事件发生时间；旧实现可退化为使用当前时间。 */
    default void recordEligibilityChange(Long orderId, String targetState, LocalDateTime occurredAt) {
        recordEligibilityChange(orderId, targetState);
    }

    default void recordEligibilityChange(OrderEligibilityChangedEvent event) {
        recordEligibilityChange(event.getOrderId(), event.getTargetState());
    }

    CandidatePage search(CandidateSearchQuery query);

    CandidateReconcileResult reconcile(Long orderIdFrom, Long orderIdTo);
}
