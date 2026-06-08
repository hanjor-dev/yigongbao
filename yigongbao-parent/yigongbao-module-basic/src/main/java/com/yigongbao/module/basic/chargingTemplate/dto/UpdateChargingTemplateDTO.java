package com.yigongbao.module.basic.chargingTemplate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新收费模板 DTO
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class UpdateChargingTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注长度不能超过512")
    private String remark;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 模板明细列表
     */
    @NotNull(message = "模板明细不能为空")
    @Size(min = 1, message = "至少包含一个收费项目")
    @Valid
    private List<ChargingTemplateItemDTO> items;
}
