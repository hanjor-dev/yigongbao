package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打印信息 VO（对应 design_product 一行）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignProductVO {

    private Long id;
    private Long orderId;
    private Long packageId;
    private Long productId;
    private String productName;
    private Long specId;
    private String specName;
    private String certNo;
    private String materialId;
    private String materialName;
    private String colorId;
    private String colorName;
    private Integer quantity;
    private Integer packQuantity;
    private String timeliness;
    private String productMark;
    private Long packageFileId;
    private String packageFileName;
    private Integer sortOrder;
}
