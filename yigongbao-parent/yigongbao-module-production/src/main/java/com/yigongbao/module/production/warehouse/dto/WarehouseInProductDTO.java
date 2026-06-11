package com.yigongbao.module.production.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 产品入库请求 DTO
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class WarehouseInProductDTO {
    @NotBlank(message = "库位不能为空")
    private String location;

    private String remark;
}
