package com.yigongbao.module.production.record.vo;

import lombok.Data;

import java.util.List;

/**
 * 生产流转卡列配置 VO
 *
 * @author hanjor
 * @date 2026-06-10
 */
@Data
public class ProductionColumnConfigVO {

    private String module = "production";

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
