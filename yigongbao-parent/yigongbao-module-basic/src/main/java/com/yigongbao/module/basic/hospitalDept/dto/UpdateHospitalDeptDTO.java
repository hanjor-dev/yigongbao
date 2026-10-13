package com.yigongbao.module.basic.hospitalDept.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新医院科室 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class UpdateHospitalDeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科室名称
     */
    private String hospitalDeptName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
