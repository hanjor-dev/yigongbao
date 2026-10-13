package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DesignerAssignedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderCode;
    private final String patientName;
    private final String hospitalName;
    private final Long newDesignerId;
    private final Long oldDesignerId;

    public DesignerAssignedEvent(Object source, Long orderId, String orderCode,
                                 String patientName, String hospitalName,
                                 Long newDesignerId, Long oldDesignerId) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.patientName = patientName;
        this.hospitalName = hospitalName;
        this.newDesignerId = newDesignerId;
        this.oldDesignerId = oldDesignerId;
    }
}
