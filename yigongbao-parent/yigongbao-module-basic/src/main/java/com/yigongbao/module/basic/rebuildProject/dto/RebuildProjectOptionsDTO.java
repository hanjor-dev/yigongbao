package com.yigongbao.module.basic.rebuildProject.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 重建项目下拉选项查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class RebuildProjectOptionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID
     */
    private Long bodyPartId;

    /**
     * 项目分类
     */
    private String category;
}
