package com.yigongbao.module.order.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单导出字段定义 VO
 *
 * @author hanjor
 * @date 2026-06-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExportFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段标识（用于导出时指定字段）
     */
    private String field;

    /**
     * 字段显示名称（前端展示给用户）
     */
    private String label;
}
