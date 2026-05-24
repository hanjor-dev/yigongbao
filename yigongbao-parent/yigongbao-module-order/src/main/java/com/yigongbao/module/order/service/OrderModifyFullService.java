package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;

/**
 * 订单全量修改 Service
 * 前端传入完整订单数据，后端自动判断变更内容
 *
 * @author hanjor
 * @date 2026-05-22
 */
public interface OrderModifyFullService {

    /**
     * 全量修改订单
     * 前端传入完整订单数据，后端自动 diff 判断变更内容并应用
     *
     * @param orderId 订单ID
     * @param dto     完整订单数据
     */
    void modifyOrderFull(Long orderId, OrderModifyFullDTO dto);
}
