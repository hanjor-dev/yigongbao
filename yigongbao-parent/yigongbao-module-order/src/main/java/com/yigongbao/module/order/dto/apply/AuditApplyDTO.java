package com.yigongbao.module.order.dto.apply;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 审核修改申请 DTO
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class AuditApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_REJECTED = 2;

    /**
     * 审核结果：1=通过，2=驳回
     */
    @NotNull(message = "审核结果不能为空")
    private Integer result;

    /**
     * 审核备注（驳回原因）
     */
    private String remark;
}
