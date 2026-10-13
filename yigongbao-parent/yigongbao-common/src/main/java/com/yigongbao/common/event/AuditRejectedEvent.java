package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditRejectedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderCode;
    private final String patientName;
    private final String hospitalName;
    private final Long createBy;
    private final String rejectReason;

    public AuditRejectedEvent(Object source, Long orderId, String orderCode,
                               String patientName, String hospitalName,
                               Long createBy, String rejectReason) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.patientName = patientName;
        this.hospitalName = hospitalName;
        this.createBy = createBy;
        this.rejectReason = rejectReason;
    }
}
