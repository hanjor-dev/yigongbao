package com.yigongbao.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程状态历史 Entity
 * 记录订单在各阶段的状态变更历史，用于追溯和审计
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
@TableName("order_flow_status_history")
@EqualsAndHashCode(callSuper = true)
public class FlowStatusHistoryEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 主键与公共字段（继承自BaseEntity） ====================
    // id, createTime, updateTime, createBy, updateBy, isDeleted

    // ==================== 订单基本信息 ====================
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderCode;

    // ==================== 状态变更信息 ====================
    /**
     * 变更时阶段
     */
    private Integer phase;

    /**
     * 变更时阶段名称（快照，历史展示用）
     */
    private String phaseName;

    /**
     * 变更前状态
     */
    private Integer fromStatus;

    /**
     * 变更前状态名称（快照，历史展示用）
     */
    private String fromStatusName;

    /**
     * 变更后状态
     */
    private Integer toStatus;

    /**
     * 变更后状态名称（快照，历史展示用）
     */
    private String toStatusName;

    /**
     * 触发动作（如 SUBMIT_ORDER、DATA_AUDIT_PASS）
     */
    private String action;

    /**
     * 动作名称
     */
    private String actionName;

    // ==================== 操作人信息 ====================
    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 备注（如驳回原因）
     */
    private String remark;
}
