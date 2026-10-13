package com.yigongbao.module.basic.product.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品列表查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class ProductListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品名称（模糊查询）
     */
    private String productName;

    /**
     * 产品分类
     */
    private String category;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
