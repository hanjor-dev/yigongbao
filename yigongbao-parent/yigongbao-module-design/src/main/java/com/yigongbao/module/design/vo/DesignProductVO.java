package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

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
    private Integer isUrgent;
    private Integer sortOrder;

    /**
     * 关联文件列表
     */
    private List<ProductFileVO> files;

    /**
     * 产品关联文件 VO
     */
    @Data
    public static class ProductFileVO {
        /** design_product_file.id */
        private Long id;
        /** design_package_file.id */
        private Long packageFileId;
        /** 文件名 */
        private String packageFileName;
    }
}
