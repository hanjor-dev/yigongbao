package com.yigongbao.module.order.dto.diff;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单项差异
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ItemsDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新增的订单项
     */
    private List<OrderItemSummary> added;

    /**
     * 删除的订单项
     */
    private List<OrderItemSummary> deleted;

    /**
     * 修改的订单项
     */
    private List<OrderItemSummary> modified;

    /**
     * 判断是否有变更
     */
    public boolean isChanged() {
        return CollUtil.isNotEmpty(added) || CollUtil.isNotEmpty(deleted) || CollUtil.isNotEmpty(modified);
    }

    /**
     * 订单项摘要信息
     */
    @Data
    public static class OrderItemSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 项目名称
         */
        private String projectName;

        /**
         * 部位名称
         */
        private String bodyPartName;

        /**
         * 项目分类名称
         */
        private String categoryName;
    }
}
