package com.yigongbao.module.basic.device.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备状态变更日志视图对象
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class DeviceStateLogVO {

    /** 日志ID */
    private Long id;

    /** 设备编号 */
    private String deviceId;

    /** 变更前状态（0=空闲，1=占用） */
    private Integer oldState;

    /** 变更后状态（0=空闲，1=占用） */
    private Integer newState;

    /** 状态变更时间 */
    private LocalDateTime changeTime;

    /** 变更类型（auto=WebSocket自动更新，manual=手动更新） */
    private String changeType;

    /** 操作人ID（手动变更时记录，自动变更为 null） */
    private Long operatorId;
}
