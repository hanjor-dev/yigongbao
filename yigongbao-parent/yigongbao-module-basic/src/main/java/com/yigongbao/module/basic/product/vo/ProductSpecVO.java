package com.yigongbao.module.basic.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品规格 VO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class ProductSpecVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联产品ID
     */
    private Long productId;

    /**
     * 规格名称
     */
    private String specName;

    /**
     * 关联注册证ID
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
}
