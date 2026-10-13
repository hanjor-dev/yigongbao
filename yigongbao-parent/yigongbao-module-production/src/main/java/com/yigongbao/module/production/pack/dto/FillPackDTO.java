package com.yigongbao.module.production.pack.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 填写包装信息 DTO
 * 使用统一的工序参数格式
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class FillPackDTO {
    /** 主设备ID（包装设备） */
    @NotNull(message = "设备ID不能为空")
    private Long primaryDeviceId;

    /** 辅助设备ID（可选） */
    private Long secondaryDeviceId;

    /** 工序参数（JSON格式，包含包装相关参数） */
    private String processParams;
}
