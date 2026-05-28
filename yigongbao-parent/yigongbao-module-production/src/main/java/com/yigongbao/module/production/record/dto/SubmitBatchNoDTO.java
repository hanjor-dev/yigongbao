package com.yigongbao.module.production.record.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 提交生产批号 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class SubmitBatchNoDTO {
    @NotBlank(message = "生产批号不能为空")
    private String productionBatchNo;
    /** 原材料批号 */
    private String materialBatchNo;
}
