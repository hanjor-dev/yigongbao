package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import com.yigongbao.module.order.vo.order.OrderListVO;

import java.util.List;

/**
 * 订单修改 Service
 * 提供直接修改订单和查询修改留痕功能
 *
 * @author hanjor
 * @date 2026-04-08
 */
public interface OrderModifyApplyService {

    /**
     * 直接修改订单（无需申请审核流程）
     * 根据订单当前阶段判断允许的修改类型：
     * - 订单阶段（phase=10）：允许全部三种类型（14.1/14.2/14.3）
     * - 设计阶段（phase=20）：仅允许重建项目（14.3）
     *
     * @param orderId 订单ID
     * @param dto     修改内容
     */
    void directModify(Long orderId, ExecuteModifyDTO dto);

    /**
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto);
}
