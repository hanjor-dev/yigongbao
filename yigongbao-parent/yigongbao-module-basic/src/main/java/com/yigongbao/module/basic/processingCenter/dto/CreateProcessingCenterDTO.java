package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CreateProcessingCenterDTO {
    @NotBlank(message = "中心编码不能为空")
    private String centerCode;

    @NotBlank(message = "中心名称不能为空")
    private String centerName;

    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private String remark;
}
