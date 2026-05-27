package com.yigongbao.module.production.qc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QcProductDTO {
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    private String result;
    private String remark;
}
