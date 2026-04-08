package com.yigongbao.module.order.dto.modify;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 审核订单修改申请 DTO
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class AuditModifyApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核操作：APPROVE-同意，REJECT-拒绝
     */
    @NotBlank(message = "审核操作不能为空")
    private String action;

    /**
     * 驳回原因（审核拒绝时必填）
     */
    private String rejectReason;
}
