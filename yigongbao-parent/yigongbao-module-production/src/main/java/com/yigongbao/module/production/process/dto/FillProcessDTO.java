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
    /** 工序参数（JSON格式） */
    private String processParams;
    /** 本工序是否有重做（0=否，1=是） */
    private Integer hasRedo;
    /** 重做备注 */
    private String redoRemark;
}
