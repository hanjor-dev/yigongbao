package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 经典案例标记事件
 * 当订单被标记为经典案例时发布，通知各模块进行文件迁移
 */
@Getter
public class ClassicCaseMarkedEvent extends ApplicationEvent {

    private final Long orderId;
    private final String orderCode;

    public ClassicCaseMarkedEvent(Object source, Long orderId, String orderCode) {
        super(source);
        this.orderId = orderId;
        this.orderCode = orderCode;
    }
}
