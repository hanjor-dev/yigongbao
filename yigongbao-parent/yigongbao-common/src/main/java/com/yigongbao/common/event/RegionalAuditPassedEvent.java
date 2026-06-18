package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RegionalAuditPassedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderCode;
    private final String patientName;
    private final String hospitalName;
    private final Long orgId;

    public RegionalAuditPassedEvent(Object source, Long orderId, String orderCode,
                                    String patientName, String hospitalName, Long orgId) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.patientName = patientName;
        this.hospitalName = hospitalName;
        this.orgId = orgId;
    }
}
