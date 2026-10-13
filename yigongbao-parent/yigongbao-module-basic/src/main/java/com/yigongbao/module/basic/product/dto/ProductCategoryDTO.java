package com.yigongbao.module.basic.product.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品按分类查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class ProductCategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品分类
     */
    private String category;
}
