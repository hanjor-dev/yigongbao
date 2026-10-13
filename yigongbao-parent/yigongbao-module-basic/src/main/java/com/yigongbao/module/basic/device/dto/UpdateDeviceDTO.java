package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 编辑设备信息请求 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class UpdateDeviceDTO {

    @NotNull(message = "设备ID不能为空")
    private Long id;

    /** 设备名称 */
    private String deviceName;

    /** 所属加工中心ID */
    private Long centerId;

    /** 加工耗时（单位：分钟） */
    private Integer processingMinutes;

    /** 备注 */
    private String remark;
}
