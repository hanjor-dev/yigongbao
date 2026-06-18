package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 订单取消事件
 * 当订单被取消时触发，用于通知生产模块更新流转卡和产品状态
 *
 * @author hanjor
 * @date 2026-06-16
 */
@Getter
public class OrderCancelledEvent extends ApplicationEvent {

    private final Long orderId;

    public OrderCancelledEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }
}
