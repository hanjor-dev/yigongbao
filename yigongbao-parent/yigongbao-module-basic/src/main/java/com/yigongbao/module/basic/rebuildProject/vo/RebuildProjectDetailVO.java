package com.yigongbao.module.basic.rebuildProject.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 重建项目详情 VO
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class RebuildProjectDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 关联部位ID
     */
    private Long bodyPartId;

    /**
     * 关联部位名称
     */
    private String bodyPartName;

    /**
     * 父项目ID
     */
    private Long parentId;

    /**
     * 父项目名称
     */
    private String parentName;

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
     * 标准价格
     */
    private BigDecimal standardPrice;

    /**
     * 加急价格
     */
    private BigDecimal urgentPrice;

    /**
     * 项目分类编码（字典 dict_code=13）
     */
    private String categoryCode;

    /**
     * 项目分类名称
     */
    private String categoryName;

    /**
     * 预计耗时（小时）
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
     * 状态名称
     */
    private String statusName;

    /**
     * 专业方向字典编码
     */
    private String specialty;

    /**
     * 专业方向名称（来自 sys_dict）
     */
    private String specialtyName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
