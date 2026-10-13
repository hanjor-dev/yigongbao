package com.yigongbao.module.basic.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建产品 DTO
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
     * 产品大类 dict_code（如 17.1）
     */
    @NotBlank(message = "产品类型不能为空")
    private String category;

    /**
     * 大类名称（冗余字段，必填）
     * 注意：由于模块架构约束，basic 模块无法依赖 system 模块的字典服务，
     * 因此前端必须同时提供 category（字典编码）和 categoryName（字典名称）
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
