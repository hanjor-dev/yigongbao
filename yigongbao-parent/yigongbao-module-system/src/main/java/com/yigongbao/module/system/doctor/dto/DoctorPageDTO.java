package com.yigongbao.module.system.doctor.dto;

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

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String doctorName;
    private Long hospitalId;
    private Integer status;
}
