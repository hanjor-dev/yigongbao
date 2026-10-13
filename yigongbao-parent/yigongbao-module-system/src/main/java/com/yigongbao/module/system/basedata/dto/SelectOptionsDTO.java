package com.yigongbao.module.system.basedata.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一下拉选项查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class SelectOptionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据类型（area=地区，dict=字典，config_group=配置分组）
     */
    private String type;

    /**
     * 字典编码（type=dict时使用）
     */
    private String code;

    /**
     * 父级ID（type=area时使用，默认0）
     */
    private Long parentId;
}
