package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProductionCardClaimedEvent extends ApplicationEvent {
    private final Long recordId;
    private final Long claimedByUserId;

    public ProductionCardClaimedEvent(Object source, Long recordId, Long claimedByUserId) {
        super(source);
        this.recordId = recordId;
        this.claimedByUserId = claimedByUserId;
    }
}
