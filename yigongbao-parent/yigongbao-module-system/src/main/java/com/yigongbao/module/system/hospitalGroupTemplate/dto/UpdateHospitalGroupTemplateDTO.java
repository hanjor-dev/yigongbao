package com.yigongbao.module.system.hospitalGroupTemplate.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新医院组合模板请求 DTO，所有字段均为可选，仅传入需要修改的字段
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class UpdateHospitalGroupTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板名称，最大 64 字符 */
    @Size(max = 64, message = "模板名称不能超过64字符")
    private String templateName;

    /** 模板描述 */
    private String templateDesc;

    /** 关联的医院 ID 列表，传入则全量替换原有关联关系 */
    private List<Long> hospitalIds;

    /** 备注信息 */
    private String remark;
}
