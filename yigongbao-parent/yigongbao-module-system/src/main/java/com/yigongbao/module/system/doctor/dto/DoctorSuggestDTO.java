package com.yigongbao.module.system.doctor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生联想查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class DoctorSuggestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long creatorId;
    private Long hospitalId;
    private String keyword;
}
