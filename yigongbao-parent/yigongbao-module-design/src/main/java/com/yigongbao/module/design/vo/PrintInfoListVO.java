package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 打印信息列表 VO（GET /{orderId}/package/{packageId}/print-info 响应）
 * 外层包含数据包级别字段，内层为产品列表
 *
 * @author hanjor
 * @date 2026-04-24
 */
@Data
public class PrintInfoListVO {

    /**
     * 产品标识（数据包级别）
     */
    private String productMark;

    /**
     * 包装数量（数据包级别）
     */
    private Integer packQuantity;

    /**
     * 备注（数据包级别）
     */
    private String remark;

    /**
     * 产品打印信息列表
     */
    private List<DesignProductVO> items;
}
