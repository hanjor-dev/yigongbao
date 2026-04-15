package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设计审核记录 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_review")
@EqualsAndHashCode(callSuper = false)
public class DesignReviewEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核人姓名（冗余）
     */
    private String reviewerName;

    /**
     * 审核结果：0=驳回，1=通过
     */
    private Integer reviewResult;

    /**
     * 审批意见（通过时）
     */
    private String comment;

    /**
     * 驳回原因（驳回时必填）
     */
    private String rejectReason;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
}
