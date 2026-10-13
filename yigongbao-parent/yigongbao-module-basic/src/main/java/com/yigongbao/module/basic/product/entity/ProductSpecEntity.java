package com.yigongbao.module.basic.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 产品规格实体
 * 一个产品（大类）下可关联多条规格，每条规格独立关联注册证
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("product_spec")
@EqualsAndHashCode(callSuper = false)
public class ProductSpecEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联产品ID
     */
    private Long productId;

    /**
     * 规格名称
     */
    private String specName;

    /**
     * 关联注册证ID（可空）
     */
    private Long certId;

    /**
     * 注册证号（冗余）
     */
    private String certNo;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
