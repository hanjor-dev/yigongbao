package com.yigongbao.module.basic.hospitalGroupTemplate.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 更新医院组合模板 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class UpdateHospitalGroupTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @Size(max = 64, message = "模板名称不能超过64字符")
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 医院ID列表（全部替换）
     */
    private List<Long> hospitalIds;

    /**
     * 备注
     */
    private String remark;
}
