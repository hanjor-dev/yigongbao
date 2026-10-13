package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationRemarkUpdateEvent extends ApplicationEvent {
    private final String bizType;
    private final Long bizId;
    private final String category;
    private final String remark;

    public NotificationRemarkUpdateEvent(Object source, String bizType, Long bizId, String category, String remark) {
        super(source);
        this.bizType = bizType;
        this.bizId = bizId;
        this.category = category;
        this.remark = remark;
    }
}
