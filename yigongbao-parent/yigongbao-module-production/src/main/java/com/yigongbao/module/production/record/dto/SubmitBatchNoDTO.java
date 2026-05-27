package com.yigongbao.module.production.record.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SubmitBatchNoDTO {
    @NotBlank(message = "生产批号不能为空")
    private String productionBatchNo;
    private String materialBatchNo;
}
