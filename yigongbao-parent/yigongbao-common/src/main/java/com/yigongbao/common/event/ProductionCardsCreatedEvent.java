package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ProductionCardsCreatedEvent extends ApplicationEvent {
    private final List<Long> recordIds;

    public ProductionCardsCreatedEvent(Object source, List<Long> recordIds) {
        super(source);
        this.recordIds = recordIds;
    }
}
