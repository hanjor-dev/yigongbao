package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 设备实体类
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class DeviceEntity extends BaseEntity {

    /** 设备编号（唯一） */
    private String deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型 */
    private String deviceType;

    /** 所属加工中心ID */
    private Long centerId;

    /** 所属加工中心名称（冗余字段） */
    private String centerName;

    /** 设备状态（0=空闲，1=占用） */
    private Integer state;

    /** 连接状态（0=离线，1=在线） */
    private Integer connectionStatus;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 加工耗时（单位：分钟） */
    private Integer processingMinutes;

    /** 备注 */
    private String remark;
}
