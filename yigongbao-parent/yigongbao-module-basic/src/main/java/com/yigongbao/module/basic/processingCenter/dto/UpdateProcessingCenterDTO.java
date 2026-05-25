package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateProcessingCenterDTO {
    @NotNull(message = "ID不能为空")
    private Long id;

    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
}
