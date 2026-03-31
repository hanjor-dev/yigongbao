package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderStatusHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态历史 Mapper
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Mapper
public interface OrderStatusHistoryMapper extends BaseMapper<OrderStatusHistoryEntity> {
}
