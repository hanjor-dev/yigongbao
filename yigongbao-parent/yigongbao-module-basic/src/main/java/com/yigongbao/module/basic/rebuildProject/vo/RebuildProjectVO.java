package com.yigongbao.module.basic.rebuildProject.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 重建项目 VO（视图对象）
 * 用于返回给前端的重建项目树形结构数据
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class RebuildProjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 父项目ID
     */
    private Long parentId;

    /**
     * 关联部位ID
     */
    private Long bodyPartId;

    /**
     * 关联部位名称
     */
    private String bodyPartName;

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
     * 项目分类
     */
    private String category;

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

    /**
     * 子项目列表
     */
    private List<RebuildProjectVO> children;
}
