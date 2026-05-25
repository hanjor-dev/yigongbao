package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CreateDeviceDTO {
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    private String deviceName;
    private String deviceType;
    private Long centerId;
    private String remark;
}
