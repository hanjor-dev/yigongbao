package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class DeviceEntity extends BaseEntity {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Long centerId;
    private String centerName;
    private Integer state;
    private Integer connectionStatus;
    private LocalDateTime lastHeartbeat;
    private String remark;
}
