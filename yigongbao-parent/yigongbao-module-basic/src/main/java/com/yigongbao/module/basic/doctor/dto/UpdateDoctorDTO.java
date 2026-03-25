package com.yigongbao.module.basic.doctor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新医生 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class UpdateDoctorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    /**
     * 所属科室ID
     */
    private Long hospitalDeptId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
