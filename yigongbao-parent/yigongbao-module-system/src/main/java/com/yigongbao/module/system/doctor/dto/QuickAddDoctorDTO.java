package com.yigongbao.module.system.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 快速添加医生 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class QuickAddDoctorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "医生姓名不能为空")
    private String doctorName;

    private String doctorPhone;

    @NotNull(message = "所属医院不能为空")
    private Long hospitalId;
}
