package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 手动创建设备请求 DTO
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class CreateDeviceDTO {

    /** 设备编号（唯一，不可重复） */
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型（如 PRINTER_SLA、PRINTER_FDM 等） */
    private String deviceType;

    /** 所属加工中心ID */
    private Long centerId;

    /** 备注 */
    private String remark;
}
