package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderModifyApplyRejectedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long applyUserId;
    private final String rejectReason;

    public OrderModifyApplyRejectedEvent(Object source, Long applyId, Long applyUserId, String rejectReason) {
        super(source);
        this.applyId = applyId;
        this.applyUserId = applyUserId;
        this.rejectReason = rejectReason;
    }
}
