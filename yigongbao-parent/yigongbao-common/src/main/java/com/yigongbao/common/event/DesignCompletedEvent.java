package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设计完成事件
 * 当设计师完成设计时触发，用于通知生产模块创建流转卡
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Getter
public class DesignCompletedEvent extends ApplicationEvent {

    /**
     * 订单ID
     */
    private final Long orderId;

    public DesignCompletedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }
}
