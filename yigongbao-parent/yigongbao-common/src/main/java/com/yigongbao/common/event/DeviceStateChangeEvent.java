package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设备状态变更事件
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
public class DeviceStateChangeEvent extends ApplicationEvent {

    private final Long deviceId;
    private final Integer oldState;
    private final Integer newState;

    public DeviceStateChangeEvent(Object source, Long deviceId, Integer oldState, Integer newState) {
        super(source);
        this.deviceId = deviceId;
        this.oldState = oldState;
        this.newState = newState;
    }
}
