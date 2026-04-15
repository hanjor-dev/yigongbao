package com.yigongbao.module.design.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存打印信息请求 DTO（整包替换）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class SavePrintInfoDTO {

    /**
     * 打印信息列表，允许为空列表（表示清空该包所有打印信息）
     */
    @NotNull(message = "items 不能为 null")
    @Valid
    private List<SavePrintInfoItemDTO> items;
}
