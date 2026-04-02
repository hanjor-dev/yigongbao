package com.yigongbao.module.basic.hospitalGroupTemplate.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医院组合模板分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class HospitalGroupTemplatePageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 模板名称（模糊查询）
     */
    private String templateName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
