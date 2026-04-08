package com.yigongbao.module.basic.rebuildProject.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 重建项目按部位查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class RebuildProjectByBodyPartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID
     */
    private Long bodyPartId;

    /**
     * 项目分类编码（字典 dict_code=13，传入则精确匹配）
     */
    private String categoryCode;
}
