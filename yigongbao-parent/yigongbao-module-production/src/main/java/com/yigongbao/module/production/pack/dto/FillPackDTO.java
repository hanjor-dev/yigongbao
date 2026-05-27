package com.yigongbao.module.production.pack.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 填写包装信息 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class FillPackDTO {
    @NotNull(message = "包装设备ID不能为空")
    private Long packDeviceId;
    private BigDecimal packSealTemperature;
    private Integer packSealTime;
    private String packSterilizationMethod;
    private String packSterilizationBatchNo;
}
