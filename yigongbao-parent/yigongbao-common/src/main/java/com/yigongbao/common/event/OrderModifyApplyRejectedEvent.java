package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderModifyApplyRejectedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long operatorId;
    private final String rejectReason;

    public OrderModifyApplyRejectedEvent(Object source, Long applyId, Long operatorId, String rejectReason) {
        super(source);
        this.applyId = applyId;
        this.operatorId = operatorId;
        this.rejectReason = rejectReason;
    }
}
