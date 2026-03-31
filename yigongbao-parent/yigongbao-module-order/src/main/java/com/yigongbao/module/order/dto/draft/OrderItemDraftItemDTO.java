package com.yigongbao.module.order.dto.draft;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 草稿重建项目明细 DTO
 * 嵌套在 CreateOrderDraftDTO.items 中
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderItemDraftItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 草稿明细ID，更新时传入
     */
    private Long id;

    /**
     * 部位ID
     */
    private Long bodyPartId;

    /**
     * 部位名称
     */
    private String bodyPartName;

    /**
     * 重建项目ID
     */
    private Long projectId;

    /**
     * 重建项目名称
     */
    private String projectName;

    /**
     * 预计耗时（小时，支持小数）
     */
    @DecimalMin(value = "0", message = "预计耗时不能为负数")
    private BigDecimal projectEstimatedHours;

    /**
     * 项目说明
     */
    private String projectDesc;

    /**
     * 成形需求
     */
    private String formingRequirement;

    /**
     * 其他要求
     */
    private String otherRequirement;

    /**
     * 排序序号
     */
    @Min(value = 1, message = "排序序号最小为1")
    private Integer sortOrder;
}
