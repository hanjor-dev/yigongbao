package com.yigongbao.module.basic.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 产品型号 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("product")
@EqualsAndHashCode(callSuper = false)
public class ProductEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品型号编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品分类（如：髋关节、膝关节、脊柱）
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
    private BigDecimal price;

    /**
     * 产品图片URL
     */
    private String imageUrl;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
