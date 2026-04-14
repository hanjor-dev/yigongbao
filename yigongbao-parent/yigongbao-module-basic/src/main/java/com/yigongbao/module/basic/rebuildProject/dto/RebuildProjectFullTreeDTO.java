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
     * 项目分类编码（字典 dict_code=13，可选，传入则精确匹配）
     */
    private String categoryCode;
}
