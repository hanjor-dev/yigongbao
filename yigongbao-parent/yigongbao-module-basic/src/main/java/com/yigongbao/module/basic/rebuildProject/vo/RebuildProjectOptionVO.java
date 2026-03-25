package com.yigongbao.module.basic.rebuildProject.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 重建项目下拉选项 VO
 * 按部位分组的项目下拉选择数据结构
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class RebuildProjectOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID
     */
    private Long bodyPartId;

    /**
     * 部位名称
     */
    private String bodyPartName;

    /**
     * 项目列表
     */
    private List<ProjectOptionItemVO> children;
}
