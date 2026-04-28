package com.yigongbao.module.system.hospitalGroupTemplate.dto;

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

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String templateName;
    private Integer status;
}
