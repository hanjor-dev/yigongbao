package com.yigongbao.module.system.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院组合模板明细 VO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCode;
    private String fullAreaName;
    private String hospitalLevelName;
    private String contact;
    private String phone;
    private Boolean assigned;
    private LocalDateTime createTime;
}
