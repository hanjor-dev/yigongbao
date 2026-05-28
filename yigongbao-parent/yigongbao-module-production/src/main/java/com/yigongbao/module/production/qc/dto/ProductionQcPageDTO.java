package com.yigongbao.module.production.qc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 质检列表分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionQcPageDTO {

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    /** 关键词：模糊匹配订单号、数据包编号、流转卡编号、患者姓名 */
    private String keyword;

    /** 流转卡状态，不传默认查质检中（5010） */
    private Integer status;
}

