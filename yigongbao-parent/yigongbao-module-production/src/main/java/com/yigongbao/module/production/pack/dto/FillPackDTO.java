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
    /** 热封温度（℃） */
    private BigDecimal packSealTemperature;
    /** 热封时间（秒） */
    private Integer packSealTime;
    /** 灭菌方式（如：环氧乙烷灭菌） */
    private String packSterilizationMethod;
    /** 灭菌批号 */
    private String packSterilizationBatchNo;
}
