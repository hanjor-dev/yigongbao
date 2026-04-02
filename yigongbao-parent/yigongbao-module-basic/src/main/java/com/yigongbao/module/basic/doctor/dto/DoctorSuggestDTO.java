package com.yigongbao.module.basic.doctor.dto;

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

    /**
     * 业务员ID
     */
    private Long creatorId;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 关键词（模糊查询医生姓名）
     */
    private String keyword;
}
