package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderDraftFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单草稿文件关联 Mapper
 *
 * @author hanjor
 * @date 2026-06-09
 */
@Mapper
public interface OrderDraftFileMapper extends BaseMapper<OrderDraftFileEntity> {
}
