package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.OrderItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 订单明细 ServiceImpl
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Service
@Slf4j
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemEntity> implements OrderItemService {

    /**
     * 查询指定订单的所有明细
     *
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    @Override
    public List<OrderItemEntity> listByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .eq(OrderItemEntity::getIsDeleted, StatusConstants.NOT_DELETED));
    }

    /**
     * 批量查询多个订单的明细（用于列表页避免 N+1）
     *
     * @param orderIds 订单ID列表
     * @return 订单明细列表
     */
    @Override
    public List<OrderItemEntity> listByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<OrderItemEntity>()
                .in(OrderItemEntity::getOrderId, orderIds)
                .eq(OrderItemEntity::getIsDeleted, StatusConstants.NOT_DELETED));
    }
}
