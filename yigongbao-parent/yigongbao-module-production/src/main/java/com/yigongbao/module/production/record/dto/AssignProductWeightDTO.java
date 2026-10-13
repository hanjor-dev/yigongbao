package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分配打印机时提交的生产产品重量
 *
 * @author hanjor
 * @date 2026-08-04
 */
@Data
public class AssignProductWeightDTO {
    /** 生产产品ID */
    @NotNull(message = "生产产品ID不能为空")
    private Long productId;

    /** 产品重量，单位：克；允许为空 */
    @DecimalMin(value = "0", inclusive = true, message = "产品重量不能小于0克")
    @Digits(integer = 8, fraction = 2, message = "产品重量最多支持8位整数和2位小数")
    private BigDecimal weight;
}
