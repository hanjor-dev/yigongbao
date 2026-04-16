package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.entity.OrderItemEntity;

import java.util.List;

/**
 * 订单明细 Service
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    /**
     * 查询指定订单的所有明细
     *
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    List<OrderItemEntity> listByOrderId(Long orderId);

    /**
     * 批量查询多个订单的明细（用于列表页避免 N+1）
     *
     * @param orderIds 订单ID列表
     * @return 订单明细列表
     */
    List<OrderItemEntity> listByOrderIds(List<Long> orderIds);
}
