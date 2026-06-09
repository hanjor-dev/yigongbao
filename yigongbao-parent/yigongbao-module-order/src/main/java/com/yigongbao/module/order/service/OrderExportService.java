package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.order.OrderCustomExportDTO;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.vo.order.OrderExportFieldVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

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

    /**
     * 自定义字段导出订单列表为 Excel
     * 用户指定时间范围和导出字段，最多导出10000条
     *
     * @param dto 自定义导出参数
     * @param response HTTP 响应
     */
    void customExportOrders(OrderCustomExportDTO dto, HttpServletResponse response);

    /**
     * 获取可用的导出字段列表
     *
     * @return 可导出字段列表
     */
    List<OrderExportFieldVO> getAvailableExportFields();
}
