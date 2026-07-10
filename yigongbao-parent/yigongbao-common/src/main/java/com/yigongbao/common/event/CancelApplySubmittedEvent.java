package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 取消申请提交事件
 * 当用户提交订单取消申请时触发，用于通知相关人员审核
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Getter
public class CancelApplySubmittedEvent extends ApplicationEvent {

    private final Long applyId;
    private final Long orderId;
    private final Long applyBy;

    public CancelApplySubmittedEvent(Object source, Long applyId, Long orderId, Long applyBy) {
        super(source);
        this.applyId = applyId;
        this.orderId = orderId;
        this.applyBy = applyBy;
    }
}
