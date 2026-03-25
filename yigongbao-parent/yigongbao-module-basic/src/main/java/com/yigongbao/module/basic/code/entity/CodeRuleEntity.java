package com.yigongbao.module.basic.code.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 编码规则 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("sys_code_rule")
@EqualsAndHashCode(callSuper = false)
public class CodeRuleEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则编码（如：ORDER_NO）
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 前缀（如：ORD-）
     */
    private String prefix;

    /**
     * 日期格式（支持 {yyyy}{MM}{dd} 等）
     */
    private String dateFormat;

    /**
     * 序号长度（不够补0）
     */
    private Integer seqLength;

    /**
     * 重置类型（DAY/MONTH/YEAR/NEVER）
     */
    private String resetType;

    /**
     * 当前序号值
     */
    private Long currentValue;

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
