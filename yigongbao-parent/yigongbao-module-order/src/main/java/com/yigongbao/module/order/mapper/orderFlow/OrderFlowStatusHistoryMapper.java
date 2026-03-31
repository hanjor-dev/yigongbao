package com.yigongbao.module.order.mapper.orderFlow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.orderFlow.OrderFlowStatusHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单流程状态历史 Mapper
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Mapper
public interface OrderFlowStatusHistoryMapper extends BaseMapper<OrderFlowStatusHistoryEntity> {
}
