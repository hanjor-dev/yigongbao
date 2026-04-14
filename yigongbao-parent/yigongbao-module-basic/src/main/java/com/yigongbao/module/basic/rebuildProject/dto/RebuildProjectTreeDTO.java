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
     * 项目分类编码（字典 dict_code=13，传入则精确匹配）
     */
    private String categoryCode;

    /**
     * 项目名称关键字（可选，传入则模糊匹配项目名称）
     */
    private String keyword;
}
