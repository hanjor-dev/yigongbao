package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 颜色分组 VO（按产品大类分组）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class ColorGroupVO {

    /** 产品大类 dict_code（如 17.1，来自二级节点的 dictValue） */
    private String categoryCode;

    /** 产品大类名称（如 模型类） */
    private String categoryName;

    /** 该大类下的颜色选项 */
    private List<DictOptionVO> colors;
}
