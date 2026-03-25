package com.yigongbao.module.basic.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建产品型号 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class CreateProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    /**
     * 产品分类
     */
    private String category;

    /**
     * 规格
     */
    private String spec;

    /**
     * 关联注册证ID
     */
    private Long certId;

    /**
     * 材质
     */
    private String material;

    /**
     * 可选颜色（JSON数组）
     */
    private String colorOptions;

    /**
     * 标准价格
     */
    private java.math.BigDecimal price;

    /**
     * 产品图片URL
     */
    private String imageUrl;

    /**
     * 备注
     */
    private String remark;
}
