package com.yigongbao.module.production.product.vo;

import lombok.Data;

/**
 * 生产产品 VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionProductVO {
    private Long id;
    private String productNo;
    private String productName;
    private String status;
    private String statusName;
    private String qcResult;
    private String udiCode;
}
