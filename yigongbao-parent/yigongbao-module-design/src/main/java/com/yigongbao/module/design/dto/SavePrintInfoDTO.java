package com.yigongbao.module.design.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
     * 产品标识（数据包级别，必填）
     */
    @NotBlank(message = "产品标识不能为空")
    private String productMark;

    /**
     * 包装数量（数据包级别统计值，选填）
     */
    private Integer packQuantity;

    /**
     * 备注（选填）
     */
    private String remark;

    /**
     * 打印信息列表，允许为空列表（表示清空该包所有打印信息）
     */
    @NotNull(message = "items 不能为 null")
    @Valid
    private List<SavePrintInfoItemDTO> items;
}
