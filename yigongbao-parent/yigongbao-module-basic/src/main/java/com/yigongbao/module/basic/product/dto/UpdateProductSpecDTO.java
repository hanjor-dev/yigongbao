package com.yigongbao.module.basic.product.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新产品规格 DTO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class UpdateProductSpecDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
