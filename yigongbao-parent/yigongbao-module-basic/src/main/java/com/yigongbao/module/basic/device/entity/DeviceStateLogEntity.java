package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_state_log")
public class DeviceStateLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private Integer oldState;
    private Integer newState;
    private LocalDateTime changeTime;
    private String changeType;
    private Long operatorId;
}
