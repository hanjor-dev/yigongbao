package com.yigongbao.module.system.basedata.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一下拉树形查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class SelectTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据类型（area=地区，dict=字典）
     */
    private String type;

    /**
     * 字典编码（type=dict时必填）
     */
    private String code;

    /**
     * 父级ID（type=area时必填，默认0）
     */
    private Long parentId;
}
