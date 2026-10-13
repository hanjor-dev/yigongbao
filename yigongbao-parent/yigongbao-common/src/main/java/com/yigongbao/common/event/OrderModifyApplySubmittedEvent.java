package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderModifyApplySubmittedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long orderId;
    private final String orderCode;
    private final String applyUserName;
    private final Long orgId;
    private final Long applyUserId;

    public OrderModifyApplySubmittedEvent(Object source, Long applyId, Long orderId, String orderCode,
                                          String applyUserName, Long orgId, Long applyUserId) {
        super(source);
        this.applyId = applyId;
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.applyUserName = applyUserName;
        this.orgId = orgId;
        this.applyUserId = applyUserId;
    }
}
