package com.yigongbao.module.basic.rebuildProject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 更新重建项目 DTO
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class UpdateRebuildProjectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联部位ID
     */
    @NotNull(message = "关联部位ID不能为空")
    private Long bodyPartId;

    /**
     * 父项目ID
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
    private String categoryCode;

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
     * 专业方向字典编码（单值，如 "7.1"）
     */
    private String specialty;

    /**
     * 备注说明
     */
    private String remark;
}
