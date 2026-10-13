package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设计师分配记录 Entity
 *
 * @author hanjor
 * @date 2026-05-18
 */
@Data
@TableName("order_designer_assignment_log")
public class OrderDesignerAssignmentLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号（冗余字段）
     */
    private String orderCode;

    /**
     * 原设计师ID（首次分配时为NULL）
     */
    private Long oldDesignerId;

    /**
     * 原设计师姓名（冗余字段）
     */
    private String oldDesignerName;

    /**
     * 新设计师ID
     */
    private Long newDesignerId;

    /**
     * 新设计师姓名（冗余字段）
     */
    private String newDesignerName;

    /**
     * 分配类型（AUTO=自动分配，MANUAL=手动分配）
     */
    private String assignType;

    /**
     * 操作人ID（自动分配时为NULL）
     */
    private Long operatorId;

    /**
     * 操作人姓名（冗余字段）
     */
    private String operatorName;

    /**
     * 分配时间
     */
    private LocalDateTime assignTime;

    /**
     * 备注说明
     */
    private String remark;
}
