package com.yigongbao.module.order.service.orderFlow;

import com.yigongbao.common.enums.order.OrderActionEnum;
import com.yigongbao.module.order.entity.OrderMainEntity;

import java.util.List;

/**
 * 订单流程状态机 Service
 * 处理订单状态流转的核心业务逻辑，供订单及后续设计/生产模块复用
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderFlowStateMachineService {

    /**
     * 查询当前可执行的动作
     *
     * @param order 订单实体
     * @return 可执行的动作列表（动作编码列表）
     */
    List<String> getAvailableActions(OrderMainEntity order);

    /**
     * 执行状态转换
     *
     * @param order 订单实体
     * @param action 动作枚举（非动作编码字符串）
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     * @return 目标状态
     */
    Integer executeTransition(OrderMainEntity order, OrderActionEnum action,
                              Long operatorId, String operatorName, String remark);
}
