package com.yigongbao.module.backup.candidate.service;

import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.module.backup.candidate.model.CandidatePage;
import com.yigongbao.module.backup.candidate.model.CandidateReconcileResult;
import com.yigongbao.module.backup.candidate.model.CandidateSearchQuery;

/** 订单备份候选可靠事件服务。 */
public interface CandidateEventService {

    /** 在订单状态事务内追加或关闭候选事实，重复事件必须幂等。 */
    void recordEligibilityChange(Long orderId, String targetState);

    default void recordEligibilityChange(OrderEligibilityChangedEvent event) {
        recordEligibilityChange(event.getOrderId(), event.getTargetState());
    }

    CandidatePage search(CandidateSearchQuery query);

    CandidateReconcileResult reconcile(Long orderIdFrom, Long orderIdTo);
}
