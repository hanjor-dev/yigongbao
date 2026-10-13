package com.yigongbao.module.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审核信息 VO
 * 用于封装订单审核环节的详细信息（区域审核/设计审核）
 *
 * @author hanjor
 * @date 2026-06-04
 */
@Data
public class AuditInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核状态：0-未审核，1-已通过，2-已驳回
     */
    private Integer status;

    /**
     * 状态描述："待审核" / "已通过" / "已驳回"
     */
    private String statusDesc;

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

    /**
     * 审核备注（驳回原因）
     */
    private String remark;
}
