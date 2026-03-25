package com.yigongbao.module.basic.bodyPart.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 重建部位下拉选项 VO
 * 用于返回给前端的身体部位下拉选择数据
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class BodyPartOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 部位名称
     */
    private String name;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 子节点列表
     */
    private List<BodyPartOptionVO> children;
}
