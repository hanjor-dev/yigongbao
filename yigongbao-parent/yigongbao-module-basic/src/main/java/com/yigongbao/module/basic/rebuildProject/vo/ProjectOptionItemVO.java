package com.yigongbao.module.basic.rebuildProject.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 项目下拉选项项 VO
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class ProjectOptionItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 父项目ID
     */
    private Long parentId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 子项目列表
     */
    private List<ProjectOptionItemVO> children;
}
