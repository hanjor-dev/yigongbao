package com.yigongbao.module.basic.chargingTemplate.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收费模板明细 DTO
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ChargingTemplateItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 重建项目ID
     */
    @NotNull(message = "重建项目ID不能为空")
    private Long rebuildProjectId;

    /**
     * 收费价格（元）
     */
    @NotNull(message = "收费价格不能为空")
    @DecimalMin(value = "0.01", message = "收费价格必须大于0")
    @DecimalMax(value = "9999999.99", message = "收费价格不能超过9999999.99")
    private BigDecimal price;
}
