package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分配打印机DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class AssignDeviceDTO {
    @NotNull(message = "打印机ID不能为空")
    private Long deviceId;
}
