package com.yigongbao.module.backup.candidate.listener;

import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.module.backup.candidate.service.CandidateEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在订单事务提交前同步写入候选事实。
 * <p>禁止异步：候选写入失败必须让订单状态事务回滚，避免出现线上状态已变更但备份发现永久漏单。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEligibilityChangedListener {

    private final CandidateEventService candidateEventService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onEligibilityChanged(OrderEligibilityChangedEvent event) {
        candidateEventService.recordEligibilityChange(event.getOrderId(), event.getTargetState(),
                event.getOccurredAt());
        log.debug("订单备份资格事件已写入候选事实: orderId={}, targetState={}",
                event.getOrderId(), event.getTargetState());
    }
}
