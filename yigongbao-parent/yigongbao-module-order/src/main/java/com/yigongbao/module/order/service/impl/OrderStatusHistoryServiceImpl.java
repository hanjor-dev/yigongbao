package com.yigongbao.module.order.service.impl;

import com.yigongbao.module.order.entity.OrderStatusHistoryEntity;
import com.yigongbao.module.order.mapper.OrderStatusHistoryMapper;
import com.yigongbao.module.order.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单状态历史 Service 实现
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    private final OrderStatusHistoryMapper orderStatusHistoryMapper;

    @Override
    public void recordTransition(Long orderId, String orderCode, Integer phase, Integer status,
                                  String action, Long operatorId, String operatorName, String remark) {
        log.info("记录订单状态变更，orderId={}, orderCode={}, phase={}, status={}, action={}, operatorId={}",
                orderId, orderCode, phase, status, action, operatorId);
        OrderStatusHistoryEntity entity = new OrderStatusHistoryEntity();
        entity.setOrderId(orderId);
        entity.setOrderCode(orderCode);
        entity.setPhase(phase);
        entity.setStatus(status);
        entity.setAction(action);
        entity.setOperatorId(operatorId);
        entity.setOperatorName(operatorName);
        entity.setRemark(remark);
        orderStatusHistoryMapper.insert(entity);
        log.info("记录订单状态变更成功，id={}", entity.getId());
    }
}
