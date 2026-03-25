package com.yigongbao.module.basic.code.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新编码规则 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class UpdateCodeRuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 日期格式
     */
    private String dateFormat;

    /**
     * 序号长度
     */
    private Integer seqLength;

    /**
     * 重置类型（DAY/MONTH/YEAR/NEVER）
     */
    private String resetType;

    /**
     * 递增步长
     */
    private Integer step;

    /**
     * 状态（0=禁用，1=启用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
