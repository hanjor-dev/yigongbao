package com.yigongbao.module.basic.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建医生 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class CreateDoctorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医生姓名
     */
    @NotBlank(message = "医生姓名不能为空")
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    /**
     * 所属医院ID
     */
    @NotNull(message = "所属医院不能为空")
    private Long hospitalId;

    /**
     * 所属科室ID
     */
    private Long hospitalDeptId;

    /**
     * 备注
     */
    private String remark;
}
