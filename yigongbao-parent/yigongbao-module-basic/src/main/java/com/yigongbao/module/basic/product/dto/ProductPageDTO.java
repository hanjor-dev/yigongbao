package com.yigongbao.module.basic.product.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class ProductPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 产品名称（模糊查询）
     */
    private String productName;

    /**
     * 产品大类 dict_code
     */
    private String category;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
