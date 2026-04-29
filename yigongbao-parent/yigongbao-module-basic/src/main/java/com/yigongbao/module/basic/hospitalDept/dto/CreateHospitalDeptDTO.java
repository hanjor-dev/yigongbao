package com.yigongbao.module.basic.hospitalDept.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建医院科室 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class CreateHospitalDeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科室名称
     */
    @NotBlank(message = "科室名称不能为空")
    private String hospitalDeptName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;
}
