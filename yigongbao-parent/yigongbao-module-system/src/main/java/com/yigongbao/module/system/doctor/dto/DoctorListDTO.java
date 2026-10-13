package com.yigongbao.module.system.doctor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生列表查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class DoctorListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String doctorName;
    private Long hospitalId;
    private Integer status;
}
