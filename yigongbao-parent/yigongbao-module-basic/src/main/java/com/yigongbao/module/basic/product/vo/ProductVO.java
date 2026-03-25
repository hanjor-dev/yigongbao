package com.yigongbao.module.basic.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品型号 VO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 产品型号编码
     */
    private String productCode;

    /**
     * 产品名称
     */
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
     * 注册证号
     */
    private String certCode;

    /**
     * 材质
     */
    private String material;

    /**
     * 可选颜色
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
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createBy;
}
