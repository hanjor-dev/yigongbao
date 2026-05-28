package com.yigongbao.module.production.process.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 提交工序质检结果 DTO
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class SubmitProcessQcDTO {
    @NotEmpty(message = "产品检验结果不能为空")
    @Valid
    private List<ProcessProductResultDTO> productResults;
}
