package com.yigongbao.module.production.record.vo;

import lombok.Data;

/**
 * 设备配置 VO（流转卡打印设备信息，用于打印前配置确认）
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class DeviceConfigVO {
    private Long id;
    /** 流转卡编号 */
    private String recordNo;
    /** 生产批号 */
    private String productionBatchNo;
    /** 设计数据包编号 */
    private String designPackageCode;
    /** 流转卡状态 */
    private Integer status;
    /** 已分配的打印机ID */
    private Long printDeviceId;
    /** 打印机编号 */
    private String printDeviceCode;
    /** 打印机名称 */
    private String printDeviceName;
}
