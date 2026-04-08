package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单信息修改申请表 Entity
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
@TableName("order_modify_apply")
@EqualsAndHashCode(callSuper = false)
public class OrderModifyApplyEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 订单关联 ====================
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号（冗余）
     */
    private String orderCode;

    /**
     * 医院名称（冗余，来自订单，用于列表展示）
     */
    private String hospitalName;

    /**
     * 患者姓名（冗余，来自订单，用于列表展示）
     */
    private String patientName;

    // ==================== 申请信息 ====================
    /**
     * 申请类型字典编码（逗号分隔，如 "14.1,14.3"）
     */
    private String applyTypeCodes;

    /**
     * 申请类型中文名冗余（如 "基础信息、重建项目"）
     */
    private String applyTypeNames;

    /**
     * 申请原因
     */
    private String applyReason;

    // ==================== 审核信息 ====================
    /**
     * 状态：PENDING-待审核，APPROVED-已同意，REJECTED-已拒绝
     */
    private String status;

    /**
     * 驳回原因（审核不通过时必填）
     */
    private String rejectReason;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核人姓名
     */
    private String auditorName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    // ==================== 操作人 ====================
    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;
}
