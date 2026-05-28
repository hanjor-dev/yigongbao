package com.yigongbao.module.production.process.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工序产品检验结果 DTO
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class ProcessProductResultDTO {
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    /** 检验结果（pass/redo） */
    @NotNull(message = "检验结果不能为空")
    private String result;
    /** 不合格原因（result=redo 时必填） */
    private String remark;
}
