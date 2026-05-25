package com.yigongbao.module.basic.device.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeviceVO {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Long centerId;
    private String centerName;
    private Integer state;
    private Integer connectionStatus;
    private LocalDateTime lastHeartbeat;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
