package com.yigongbao.module.order.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 审核订单 DTO
 * 用于订单审核操作（通过/驳回）
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class AuditOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核备注（审核驳回时必填，用于填写驳回原因）
     */
    private String remark;

    /**
     * 预估费用（审核通过时可填写）
     */
    private BigDecimal estimatedCost;

    /**
     * 影像数据评估意见（审核时填写）
     */
    private String dataEvaluationOpinion;
}
