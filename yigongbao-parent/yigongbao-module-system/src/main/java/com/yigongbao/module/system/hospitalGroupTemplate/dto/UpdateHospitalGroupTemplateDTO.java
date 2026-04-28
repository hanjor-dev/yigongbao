package com.yigongbao.module.system.hospitalGroupTemplate.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

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

    @Size(max = 64, message = "模板名称不能超过64字符")
    private String templateName;

    private String templateDesc;

    private List<Long> hospitalIds;

    private String remark;
}
