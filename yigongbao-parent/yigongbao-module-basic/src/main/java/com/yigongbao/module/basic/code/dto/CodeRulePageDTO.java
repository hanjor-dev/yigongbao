package com.yigongbao.module.basic.code.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 编码规则分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class CodeRulePageDTO implements Serializable {

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
     * 规则编码（模糊查询）
     */
    private String ruleCode;

    /**
     * 规则名称（模糊查询）
     */
    private String ruleName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
