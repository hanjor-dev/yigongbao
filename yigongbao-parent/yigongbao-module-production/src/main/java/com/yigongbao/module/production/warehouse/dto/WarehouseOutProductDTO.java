package com.yigongbao.module.production.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 产品出库请求 DTO
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class WarehouseOutProductDTO {
    @NotBlank(message = "收货人不能为空")
    private String recipient;

    @NotBlank(message = "收货电话不能为空")
    private String recipientPhone;

    private String remark;
}
