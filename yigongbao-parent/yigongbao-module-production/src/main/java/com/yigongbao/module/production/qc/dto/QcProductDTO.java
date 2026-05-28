package com.yigongbao.module.production.qc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 质检产品 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class QcProductDTO {
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    /** 质检结果（pass/redo） */
    private String result;
    /** 不合格原因 */
    private String remark;
}
