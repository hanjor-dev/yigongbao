package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核驳回请求体
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class ReviewRejectDTO {

    /**
     * 驳回原因（必填）
     */
    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;
}
