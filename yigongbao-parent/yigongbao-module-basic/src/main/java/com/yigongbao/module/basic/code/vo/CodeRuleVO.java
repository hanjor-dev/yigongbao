package com.yigongbao.module.basic.code.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 编码规则 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class CodeRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

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
     * 重置类型
     */
    private String resetType;

    /**
     * 重置类型名称
     */
    private String resetTypeName;

    /**
     * 当前序号值
     */
    private Long currentValue;

    /**
     * 递增步长
     */
    private Integer step;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
