package com.yigongbao.module.basic.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品 VO
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
     * 产品名称
     */
    private String productName;

    /**
     * 产品大类 dict_code（如 17.1）
     */
    private String category;

    /**
     * 大类名称（冗余）
     */
    private String categoryName;

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

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 规格列表（仅在 getById 时填充）
     */
    private List<ProductSpecVO> specs;
}
