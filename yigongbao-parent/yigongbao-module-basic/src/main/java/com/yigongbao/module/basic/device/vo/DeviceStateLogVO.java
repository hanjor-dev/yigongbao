package com.yigongbao.module.basic.device.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeviceStateLogVO {
    private Long id;
    private String deviceId;
    private Integer oldState;
    private Integer newState;
    private LocalDateTime changeTime;
    private String changeType;
    private Long operatorId;
}
