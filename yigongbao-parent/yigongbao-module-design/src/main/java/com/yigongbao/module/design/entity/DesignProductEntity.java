package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印产品信息 Entity
 * 一行对应一个产品+规格，关联文件通过 design_product_file 表存储
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_product")
@EqualsAndHashCode(callSuper = false)
public class DesignProductEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 数据包ID
     */
    private Long packageId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称（冗余）
     */
    private String productName;

    /** 产品分类字典码快照 */
    private String productCategory;

    /** 产品分类名称快照 */
    private String productCategoryName;

    /**
     * 型号规格ID
     */
    private Long specId;

    /**
     * 型号规格名称（冗余）
     */
    private String specName;

    /**
     * 注册证号（冗余）
     */
    private String certNo;

    /**
     * 材质 dict_code（如 15.1）
     */
    private String materialId;

    /**
     * 材质名称（冗余）
     */
    private String materialName;

    /**
     * 颜色 dict_code（如 16.1.1）
     */
    private String colorId;

    /**
     * 颜色名称（冗余）
     */
    private String colorName;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否加急（0=普通，1=加急），默认从订单带出，允许修改
     */
    private Integer isUrgent;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
