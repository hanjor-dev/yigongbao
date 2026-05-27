package com.yigongbao.module.production.process.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 填写工序信息 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class FillProcessDTO {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    private String processParams;
    private Integer hasRedo;
    private String redoRemark;
}
