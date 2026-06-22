package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RegionalAuditPassedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderCode;
    private final String patientName;
    private final String orgName;
    private final Long regionalAuditBy;
    private final Long orgId;

    public RegionalAuditPassedEvent(Object source, Long orderId, String orderCode,
                                    String patientName, String orgName, Long regionalAuditBy, Long orgId) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.patientName = patientName;
        this.orgName = orgName;
        this.regionalAuditBy = regionalAuditBy;
        this.orgId = orgId;
    }
}
