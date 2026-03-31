package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
