package com.yigongbao.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.mapper.FlowOrderMapper;
import com.yigongbao.flow.service.FlowOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 流程订单基础 Service 实现
 * 提供订单主表的基础查询和状态更新能力，供状态机、状态历史等通用流程模块使用
 *
 * 【设计说明】
 * - 此实现类是 flow 模块与 order 模块的"解耦桥梁"
 * - flow 模块通过 FlowOrderService 接口调用，不直接依赖 order 模块的 Mapper
 * - 使用 LambdaUpdateWrapper 进行更新，避免先查后改的并发问题
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowOrderServiceImpl implements FlowOrderService {

    private final FlowOrderMapper flowOrderMapper;

    /**
     * 根据订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单实体，如果不存在返回 null
     */
    @Override
    public OrderMainEntity getById(Long id) {
        return flowOrderMapper.selectById(id);
    }

    /**
     * 更新订单阶段和状态
     * 供状态机执行完状态转换后调用，落库新的阶段和状态
     *
     * 【实现说明】
     * 使用 LambdaUpdateWrapper 直接执行 UPDATE 语句，避免先查后改的并发问题
     *
     * @param id 订单ID
     * @param phase 目标阶段
     * @param status 目标状态
     */
    @Override
    public void updatePhaseAndStatus(Long id, Integer phase, Integer status) {
        log.info("更新订单阶段和状态，orderId={}, phase={}, status={}", id, phase, status);
        try {
            LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(OrderMainEntity::getId, id)
                    .set(OrderMainEntity::getPhase, phase)
                    .set(OrderMainEntity::getStatus, status);
            flowOrderMapper.update(null, wrapper);
            log.info("更新订单阶段和状态成功，orderId={}", id);
        } catch (Exception e) {
            log.error("更新订单阶段和状态异常，orderId={}, phase={}, status={}", id, phase, status, e);
            throw e;
        }
    }

    /**
     * 更新订单阶段、状态和当前处理人
     *
     * 【使用场景】
     * 审核通过/驳回时，需要同时更新处理人信息
     *
     * @param id 订单ID
     * @param phase 目标阶段
     * @param status 目标状态
     * @param currentHandlerId 当前处理人ID
     */
    @Override
    public void updatePhaseAndStatusWithHandler(Long id, Integer phase, Integer status, Long currentHandlerId) {
        log.info("更新订单阶段和状态（带处理人），orderId={}, phase={}, status={}, handlerId={}",
                id, phase, status, currentHandlerId);
        try {
            LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(OrderMainEntity::getId, id)
                    .set(OrderMainEntity::getPhase, phase)
                    .set(OrderMainEntity::getStatus, status)
                    .set(OrderMainEntity::getCurrentHandlerId, currentHandlerId);
            flowOrderMapper.update(null, wrapper);
            log.info("更新订单阶段和状态成功，orderId={}", id);
        } catch (Exception e) {
            log.error("更新订单阶段和状态异常，orderId={}, handlerId={}", id, currentHandlerId, e);
            throw e;
        }
    }
}
