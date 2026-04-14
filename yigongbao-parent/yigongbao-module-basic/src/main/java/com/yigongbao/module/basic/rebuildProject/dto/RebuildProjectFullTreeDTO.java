package com.yigongbao.module.basic.rebuildProject.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取完整部位-项目树形结构请求 DTO
 *
 * @author hanjor
 * @date 2026-04-14
 */
@Data
public class RebuildProjectFullTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID（可选，传入则只返回该部位下的项目）
     */
    private Long bodyPartId;

    /**
     * 项目分类编码（字典 dict_code=13，可选，传入则精确匹配）
     */
    private String categoryCode;

    /**
     * 项目名称关键字（可选，传入则模糊匹配项目名称，无匹配项目的部位不返回）
     */
    private String keyword;
}
