package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单修改留痕表 Mapper
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Mapper
public interface OrderModificationLogMapper extends BaseMapper<OrderModificationLogEntity> {
}
