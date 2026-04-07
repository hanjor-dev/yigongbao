package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细表 Entity
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
@TableName("order_item")
@EqualsAndHashCode(callSuper = false)
public class OrderItemEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与关联字段 ====================
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderCode;

    // ==================== 重建项目信息 ====================
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
     * 项目分类（字典值，如"整形"、"肿瘤"等）
     */
    private String category;

    /**
     * 预计耗时（小时，支持小数）
     */
    private BigDecimal projectEstimatedHours;

    // ==================== 用户填写内容 ====================
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

    // ==================== 序号 ====================
    /**
     * 排序序号
     */
    private Integer sortOrder;
}
