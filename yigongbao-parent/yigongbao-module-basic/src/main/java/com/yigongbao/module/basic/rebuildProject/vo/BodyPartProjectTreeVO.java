package com.yigongbao.module.basic.rebuildProject.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部位-项目完整树 VO
 * 用于 full-tree 接口，按部位分组展示重建项目树形结构
 *
 * @author hanjor
 * @date 2026-04-14
 */
@Data
public class BodyPartProjectTreeVO implements Serializable {

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
     * 该部位下的重建项目列表（已构建为树形，支持二级）
     */
    private List<ProjectOptionItemVO> children;
}
