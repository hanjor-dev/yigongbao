package com.yigongbao.module.basic.hospitalDept.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医院科室列表查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class HospitalDeptListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科室名称（模糊查询）
     */
    private String hospitalDeptName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
