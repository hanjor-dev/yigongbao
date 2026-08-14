package com.yigongbao.module.basic.device.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备视图对象
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class DeviceVO {

    /** 设备ID */
    private Long id;

    /** 设备编号（唯一） */
    private String deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型（如 PRINTER_SLA、PRINTER_FDM 等） */
    private String deviceType;

    /** 所属加工中心ID */
    private Long centerId;

    /** 所属加工中心名称（冗余字段） */
    private String centerName;

    /** 设备状态（0=空闲，1=工作中，2=打印完成，3=报警，4=暂停，5=准备就绪，6=离线） */
    private Integer state;

    /** 设备状态名称 */
    private String stateName;

    /** 连接状态（0=离线，1=在线） */
    private Integer connectionStatus;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 加工耗时（单位：分钟） */
    private Integer processingMinutes;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
