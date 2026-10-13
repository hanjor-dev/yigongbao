package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备状态变更日志实体类
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
@TableName("device_state_log")
public class DeviceStateLogEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编号 */
    private String deviceId;

    /** 旧状态 */
    private Integer oldState;

    /** 新状态 */
    private Integer newState;

    /** 变更时间 */
    private LocalDateTime changeTime;

    /** 变更类型（auto=自动，manual=手动） */
    private String changeType;

    /** 操作人ID（手动变更时） */
    private Long operatorId;
}
