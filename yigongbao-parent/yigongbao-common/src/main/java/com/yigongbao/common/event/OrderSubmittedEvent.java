package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderSubmittedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderCode;
    private final String businessType;
    private final String patientName;
    private final String orgName;
    private final String operatorName;
    private final Long hospitalId;
    private final Long orgId;
    private final Long deptId;
    private final Long createBy;

    public OrderSubmittedEvent(Object source, Long orderId, String orderCode, String businessType,
                               String patientName, String orgName, String operatorName,
                               Long hospitalId, Long orgId, Long deptId, Long createBy) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.businessType = businessType;
        this.patientName = patientName;
        this.orgName = orgName;
        this.operatorName = operatorName;
        this.hospitalId = hospitalId;
        this.orgId = orgId;
        this.deptId = deptId;
        this.createBy = createBy;
    }
}
