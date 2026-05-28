package com.yigongbao.module.production.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 生产产品明细分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class ProductionProductPageDTO {

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    /**
     * 关键词：模糊匹配订单号、数据包编号、流转卡编号、产品名称、患者姓名
     */
    private String keyword;
}
