package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RegionalAuditPassedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long orgId;

    public RegionalAuditPassedEvent(Object source, Long orderId, Long orgId) {
        super(source);
        this.orderId = orderId;
        this.orgId = orgId;
    }
}
