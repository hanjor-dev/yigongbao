package com.yigongbao.module.basic.rebuildProject.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建重建项目 DTO
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class CreateRebuildProjectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联部位ID
     */
    @NotNull(message = "关联部位ID不能为空")
    private Long bodyPartId;

    /**
     * 父项目ID（0=顶级重建项目）
     */
    @NotNull(message = "父项目ID不能为空")
    private Long parentId;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100")
    private String name;

    /**
     * 标准价格
     */
    private BigDecimal standardPrice;

    /**
     * 加急价格
     */
    private BigDecimal urgentPrice;

    /**
     * 项目分类编码（字典 dict_code=13，如 13.1=模型，13.2=导板）
     */
    @NotNull(message = "项目分类不能为空")
    @Size(max = 20, message = "项目分类编码长度不能超过20")
    private String categoryCode;

    /**
     * 预计耗时（小时）
     */
    @DecimalMin(value = "0", message = "预计耗时不能为负数")
    @DecimalMax(value = "9999.99", message = "预计耗时不能超过9999.99小时")
    private BigDecimal estimatedHours;

    /**
     * 项目说明模板
     */
    @Size(max = 5000, message = "项目说明长度不能超过5000")
    private String description;

    /**
     * 成形需求模板
     */
    @Size(max = 5000, message = "成形需求长度不能超过5000")
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
     * 专业方向字典编码（单值，如 "7.1"）
     */
    @NotNull(message = "专业方向不能为空")
    @Size(max = 64, message = "专业方向编码长度不能超过64")
    private String specialty;

    /**
     * 备注说明
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
