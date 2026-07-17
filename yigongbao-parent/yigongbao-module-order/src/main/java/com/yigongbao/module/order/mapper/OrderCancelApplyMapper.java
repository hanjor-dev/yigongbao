package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.order.dto.order.CancelApplyPageQueryDTO;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单取消申请 Mapper
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Mapper
public interface OrderCancelApplyMapper extends BaseMapper<OrderCancelApplyEntity> {

    IPage<OrderCancelApplyEntity> selectPendingPage(
            Page<OrderCancelApplyEntity> page,
            @Param("query") CancelApplyPageQueryDTO query);

    IPage<OrderCancelApplyEntity> selectMyPage(
            Page<OrderCancelApplyEntity> page,
            @Param("query") CancelApplyPageQueryDTO query,
            @Param("applyBy") Long applyBy);
}
