package com.yigongbao.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 统一下拉树形 VO
 * 用于 SelectController 返回树形结构
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class SelectTreeVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 值（字典值 / 地区编码）
     */
    private String value;

    /** 可选的展示颜色，例如流程状态标签颜色 */
    private String color;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 子节点列表
     */
    private List<SelectTreeVO> children;
}
