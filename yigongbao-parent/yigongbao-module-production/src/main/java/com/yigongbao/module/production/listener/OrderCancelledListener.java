package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.event.OrderCancelledEvent;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单取消事件监听器
 * 订单取消时级联更新流转卡和产品状态为已废弃
 *
 * @author hanjor
 * @date 2026-06-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledListener {

    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) {
        Long orderId = event.getOrderId();

        List<Long> recordIds = recordMapper.selectList(
                        new LambdaQueryWrapper<ProductionRecordEntity>()
                                .eq(ProductionRecordEntity::getOrderId, orderId)
                                .ne(ProductionRecordEntity::getStatus, FlowStatusEnum.CANCELLED.getValue())
                                .select(ProductionRecordEntity::getId))
                .stream()
                .map(ProductionRecordEntity::getId)
                .collect(Collectors.toList());

        int recordCount = 0;
        int productCount = 0;

        if (!recordIds.isEmpty()) {
            LambdaUpdateWrapper<ProductionRecordEntity> recordWrapper = new LambdaUpdateWrapper<>();
            recordWrapper.in(ProductionRecordEntity::getId, recordIds)
                    .set(ProductionRecordEntity::getStatus, FlowStatusEnum.CANCELLED.getValue())
                    .set(ProductionRecordEntity::getContentUpdateTime, LocalDateTime.now());
            recordCount = recordMapper.update(null, recordWrapper);

            LambdaUpdateWrapper<ProductionProductEntity> productWrapper = new LambdaUpdateWrapper<>();
            productWrapper.in(ProductionProductEntity::getProductionRecordId, recordIds)
                    .ne(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode())
                    .set(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode());
            productCount = productMapper.update(null, productWrapper);
        }

        log.info("订单取消级联更新完成: orderId={}, 流转卡更新数={}, 产品更新数={}",
                orderId, recordCount, productCount);
    }
}
