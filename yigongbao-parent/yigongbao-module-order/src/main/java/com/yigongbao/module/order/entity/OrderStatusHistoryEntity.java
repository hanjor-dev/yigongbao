package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 订单状态历史表 Entity
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
@TableName("order_status_history")
@EqualsAndHashCode(callSuper = false)
public class OrderStatusHistoryEntity extends BaseEntity implements Serializable {

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

    // ==================== 变更信息 ====================
    /**
     * 变更前/后的阶段
     */
    private Integer phase;

    /**
     * 变更前/后的状态
     */
    private Integer status;

    /**
     * 执行的动作
     */
    private String action;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 备注（驳回原因等）
     */
    private String remark;
}
