package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditRejectedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long createBy;
    private final String rejectReason;

    public AuditRejectedEvent(Object source, Long orderId, Long createBy, String rejectReason) {
        super(source);
        this.orderId = orderId;
        this.createBy = createBy;
        this.rejectReason = rejectReason;
    }
}
