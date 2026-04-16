package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 设计工单列配置 VO（独立于订单列配置，字段内容不同）
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignColumnConfigVO {

    private String module = "design";

    private List<ColumnItemVO> columns;

    @Data
    public static class ColumnItemVO {

        /** 字段名 */
        private String field;

        /** 列标题 */
        private String label;

        /** 是否可见 */
        private Boolean visible;

        /** 排序序号 */
        private Integer sort;

        /** 列宽度（px） */
        private Integer width;

        /** 固定位置：left / right / null */
        private String fixed;
    }
}
