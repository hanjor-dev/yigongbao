package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设计审核通过事件
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Getter
public class DesignReviewPassedEvent extends ApplicationEvent {

    private final Long orderId;

    public DesignReviewPassedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }
}
