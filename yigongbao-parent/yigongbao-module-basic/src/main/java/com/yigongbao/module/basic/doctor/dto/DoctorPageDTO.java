package com.yigongbao.module.basic.doctor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class DoctorPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 医生姓名（模糊查询）
     */
    private String doctorName;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 科室ID
     */
    private Long hospitalDeptId;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
