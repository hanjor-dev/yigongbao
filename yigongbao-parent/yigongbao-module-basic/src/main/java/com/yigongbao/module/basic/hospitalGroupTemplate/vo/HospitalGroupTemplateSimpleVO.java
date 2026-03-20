package com.yigongbao.module.basic.hospitalGroupTemplate.vo;

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

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 医院数量
     */
    private Integer hospitalCount;
}
