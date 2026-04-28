package com.yigongbao.module.system.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 医院组合模板 VO（下拉选项用）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateName;
    private String templateCode;
    private Integer hospitalCount;
}
