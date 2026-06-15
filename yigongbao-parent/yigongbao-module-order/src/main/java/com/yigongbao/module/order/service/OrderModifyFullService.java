package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;

/**
 * 订单全量修改 Service
 * 前端传入完整订单数据，后端自动判断变更内容
 *
 * @author hanjor
 * @date 2026-05-22
 */
public interface OrderModifyFullService {

    /**
     * 全量修改订单
     * 前端传入完整订单数据，后端自动 diff 判断变更内容并应用
     *
     * @param orderId 订单ID
     * @param dto     完整订单数据
     */
    void modifyOrderFull(Long orderId, OrderModifyFullDTO dto);

    /**
     * 全量修改订单（可选跳过权限校验）
     * 用于审核申请场景：审核通过后应用申请内容，不校验审核人权限
     *
     * @param orderId              订单ID
     * @param dto                  完整订单数据
     * @param skipPermissionCheck  是否跳过权限校验（审核场景传true）
     */
    void modifyOrderFull(Long orderId, OrderModifyFullDTO dto, boolean skipPermissionCheck);

    /**
     * 全量修改订单（审核场景专用）
     * 跳过权限校验，并使用申请人作为修改人记录日志
     *
     * @param orderId              订单ID
     * @param dto                  完整订单数据
     * @param skipPermissionCheck  是否跳过权限校验
     * @param modifierId           修改人ID（申请人ID）
     * @param modifierName         修改人姓名（申请人姓名）
     * @param modifierRoleCode     修改人角色代码（用于判断修改范围）
     */
    void modifyOrderFull(Long orderId, OrderModifyFullDTO dto, boolean skipPermissionCheck, Long modifierId, String modifierName, String modifierRoleCode);
}
