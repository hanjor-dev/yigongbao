package com.yigongbao.module.basic.product.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新产品 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class UpdateProductDTO implements Serializable {

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
     * 大类名称（冗余字段）
     * 注意：若更新 category，必须同时提供 categoryName
     * 原因：模块架构约束，basic 模块无法依赖 system 模块的字典服务
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
