package com.yigongbao.module.basic.rebuildProject.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 重建项目树形查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class RebuildProjectTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目分类
     */
    private String category;
}
