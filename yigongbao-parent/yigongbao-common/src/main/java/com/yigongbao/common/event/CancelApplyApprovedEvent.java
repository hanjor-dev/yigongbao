package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 取消申请审核通过事件
 * 当设计管理员审核通过取消申请时触发，用于通知申请人
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Getter
public class CancelApplyApprovedEvent extends ApplicationEvent {

    private final Long applyId;
    private final Long orderId;
    private final Long auditBy;
    private final Long applyBy;

    public CancelApplyApprovedEvent(Object source, Long applyId, Long orderId, Long auditBy, Long applyBy) {
        super(source);
        this.applyId = applyId;
        this.orderId = orderId;
        this.auditBy = auditBy;
        this.applyBy = applyBy;
    }
}
