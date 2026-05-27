package com.yigongbao.module.production.record.vo;

import lombok.Data;

/**
 * 设备配置VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class DeviceConfigVO {
    private Long id;
    private String recordNo;
    private String productionBatchNo;
    private String designPackageCode;
    private String status;
    private Long printDeviceId;
    private String printDeviceCode;
    private String printDeviceName;
}
