package com.yigongbao.module.production.process.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始工序DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class StartProcessDTO {
    @NotBlank(message = "工序类型不能为空")
    private String processType;
    @NotNull(message = "主设备ID不能为空")
    private Long primaryDeviceId;
    private Long secondaryDeviceId;
    private String processParams;
}
