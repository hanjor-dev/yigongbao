package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

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
    private final LocalDateTime printStartTime;
    private final Integer estimatedDurationMinutes;
    private final LocalDateTime estimatedPrintFinishTime;

    public DeviceStateChangeEvent(Object source, Long deviceId, Integer oldState, Integer newState) {
        this(source, deviceId, oldState, newState, null, null, null);
    }

    public DeviceStateChangeEvent(Object source, Long deviceId, Integer oldState, Integer newState,
                                  LocalDateTime printStartTime, Integer estimatedDurationMinutes,
                                  LocalDateTime estimatedPrintFinishTime) {
        super(source);
        this.deviceId = deviceId;
        this.oldState = oldState;
        this.newState = newState;
        this.printStartTime = printStartTime;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.estimatedPrintFinishTime = estimatedPrintFinishTime;
    }
}
