package com.yigongbao.module.basic.hospitalGroupTemplate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建医院组合模板 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class CreateHospitalGroupTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称不能超过64字符")
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 医院ID列表
     */
    @NotEmpty(message = "请选择医院")
    private List<Long> hospitalIds;

    /**
     * 备注
     */
    private String remark;
}
