package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DesignerAssignedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long newDesignerId;
    private final Long oldDesignerId;

    public DesignerAssignedEvent(Object source, Long orderId, Long newDesignerId, Long oldDesignerId) {
        super(source);
        this.orderId = orderId;
        this.newDesignerId = newDesignerId;
        this.oldDesignerId = oldDesignerId;
    }
}
