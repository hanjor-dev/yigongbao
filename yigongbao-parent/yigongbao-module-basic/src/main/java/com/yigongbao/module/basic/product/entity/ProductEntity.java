package com.yigongbao.module.basic.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 产品实体（产品大类）
 * 产品按大类管理，规格单独维护于 ProductSpecEntity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("product")
@EqualsAndHashCode(callSuper = false)
public class ProductEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品大类 dict_code（如 17.1）
     */
    private String category;

    /**
     * 大类名称（冗余，来自字典）
     */
    private String categoryName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
