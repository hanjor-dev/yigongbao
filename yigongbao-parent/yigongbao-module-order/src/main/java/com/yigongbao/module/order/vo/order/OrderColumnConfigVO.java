package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单列配置 VO
 * 用于返回给前端的列配置结构
 *
 * @author hanjor
 * @date 2026-04-06
 */
@Data
public class OrderColumnConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 后端列配置结构版本，前端无需参与维护。 */
    private Integer version;

    /**
     * 模块标识
     */
    private String module;

    /**
     * 列配置列表
     */
    private List<ColumnItemVO> columns;

    /**
     * 列配置项 VO
     */
    @Data
    public static class ColumnItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 字段名
         */
        private String field;

        /**
         * 列标题
         */
        private String label;

        /**
         * 是否可见
         */
        private Boolean visible;

        /**
         * 排序序号
         */
        private Integer sort;

        /**
         * 列宽度
         */
        private Integer width;

        /**
         * 固定位置（left/right/null）
         */
        private String fixed;
    }
}
