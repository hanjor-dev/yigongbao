package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 订单导出 Service
 *
 * @author hanjor
 * @date 2026-04-06
 */
public interface OrderExportService {

    /**
     * 导出订单列表为 Excel
     * 导出列跟随用户自定义配置（用户个人配置 > 系统默认配置）
     * 最多导出10000条数据
     *
     * @param dto 查询参数
     * @param response HTTP 响应
     */
    void exportOrders(OrderExportQueryDTO dto, HttpServletResponse response);
}
