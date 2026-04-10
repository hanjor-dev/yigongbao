package com.yigongbao.module.basic.rebuildProject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 重建项目 Entity
 * 管理重建项目树形结构（部位 → 重建项目 → 子重建项目）
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
@TableName("rebuild_project")
@EqualsAndHashCode(callSuper = false)
public class RebuildProjectEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联部位ID
     */
    private Long bodyPartId;

    /**
     * 父项目ID（0=顶级重建项目）
     */
    private Long parentId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目编码
     */
    private String code;

    /**
     * 层级（1=重建项目，2=子重建项目）
     */
    private Integer level;

    /**
     * 标准价格（元）
     */
    private BigDecimal standardPrice;

    /**
     * 加急价格（元）
     */
    private BigDecimal urgentPrice;

    /**
     * 项目分类编码（字典 dict_code=13，如 13.1=模型）
     */
    private String categoryCode;

    /**
     * 项目分类名称（冗余字段，与字典 dict_name 一致）
     */
    private String categoryName;

    /**
     * 预计耗时（小时，支持小数）
     */
    private BigDecimal estimatedHours;

    /**
     * 项目说明模板
     */
    private String description;

    /**
     * 成形需求模板
     */
    private String formingRequirements;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 专业方向字典编码（单值，如 "7.1"，关联 sys_dict；用于自动匹配设计师）
     */
    private String specialty;

    /**
     * 备注说明
     */
    private String remark;
}
