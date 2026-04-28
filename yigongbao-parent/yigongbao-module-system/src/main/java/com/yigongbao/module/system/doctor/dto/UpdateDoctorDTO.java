package com.yigongbao.module.system.doctor.dto;

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

    private String doctorName;
    private String doctorPhone;
    private Integer status;
    private String remark;
}
