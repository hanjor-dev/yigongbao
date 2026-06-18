package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderSubmittedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String businessType;
    private final Long hospitalId;
    private final Long orgId;
    private final Long createBy;

    public OrderSubmittedEvent(Object source, Long orderId, String businessType, Long hospitalId, Long orgId, Long createBy) {
        super(source);
        this.orderId = orderId;
        this.businessType = businessType;
        this.hospitalId = hospitalId;
        this.orgId = orgId;
        this.createBy = createBy;
    }
}
