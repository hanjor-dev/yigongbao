package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单取消申请 Entity
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_cancel_apply")
public class OrderCancelApplyEntity extends BaseEntity {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 申请人ID
     */
    private Long applyBy;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 审核状态：0-待审核，1-通过，2-驳回
     */
    private Integer auditStatus;

    /**
     * 审核人ID
     */
    private Long auditBy;

    /**
     * 审核原因（驳回原因或审核备注）
     */
    private String auditReason;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
}
