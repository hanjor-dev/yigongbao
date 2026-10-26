package com.yigongbao.module.backup.candidate;

import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.module.backup.candidate.listener.OrderEligibilityChangedListener;
import com.yigongbao.module.backup.candidate.service.impl.CandidateEventServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderEligibilityChangedListenerTest {

    @Test
    void listenerUsesBeforeCommitAndCommonEventContract() throws Exception {
        Method method = OrderEligibilityChangedListener.class.getDeclaredMethod(
                "onEligibilityChanged", OrderEligibilityChangedEvent.class);
        var annotation = method.getAnnotation(org.springframework.transaction.event.TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT);

        var event = new OrderEligibilityChangedEvent(this, 1L,
                OrderEligibilityChangedEvent.FORMAL, LocalDateTime.now());
        assertThat(event.getOrderId()).isEqualTo(1L);
        assertThat(event.getTargetState()).isEqualTo("FORMAL");
    }
}
