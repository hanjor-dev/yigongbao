package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 打印信息选项 VO（GET /print-info/options 响应）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class PrintInfoOptionsVO {

    /**
     * 设计模式（来自 order_main.design_mode，暂未存储于该表则为 null）
     */
    private Integer designMode;

    /**
     * 产品树（含 specs）
     */
    private List<PrintInfoProductVO> products;

    /**
     * 材质列表
     */
    private List<DictOptionVO> materials;

    /**
     * 颜色分组（按产品大类分组）
     */
    private List<ColorGroupVO> colorGroups;
}
