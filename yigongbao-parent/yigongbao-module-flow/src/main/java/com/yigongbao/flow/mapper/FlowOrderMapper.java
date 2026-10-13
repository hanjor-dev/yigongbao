package com.yigongbao.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表 Mapper
 * 供 flow 模块的 FlowOrderService 使用
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Mapper
public interface FlowOrderMapper extends BaseMapper<OrderMainEntity> {
}
