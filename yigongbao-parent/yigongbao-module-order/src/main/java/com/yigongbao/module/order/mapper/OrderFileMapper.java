package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单文件关联 Mapper
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Mapper
public interface OrderFileMapper extends BaseMapper<OrderFileEntity> {
}
