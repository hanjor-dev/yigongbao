package com.yigongbao.module.production.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始工序 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class StartProcessDTO {
    /** 工序类型代码（wash/cure/clean_dry/pack） */
    @NotBlank(message = "工序类型不能为空")
    private String processType;
    /** 主设备ID */
    @NotNull(message = "主设备ID不能为空")
    private Long primaryDeviceId;
    /** 辅助设备ID（可选） */
    private Long secondaryDeviceId;
    /** 工序参数（JSON格式） */
    private String processParams;
}
