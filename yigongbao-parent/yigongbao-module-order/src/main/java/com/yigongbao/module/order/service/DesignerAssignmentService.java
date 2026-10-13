package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.vo.order.DesignerVO;

import java.util.List;

/**
 * 设计师分配 Service
 *
 * @author hanjor
 * @date 2026-04-10
 */
public interface DesignerAssignmentService {

    /**
     * 审核通过后触发分配（根据系统配置决定自动或跳过；分配失败不影响审核结果）
     *
     * @param orderId 订单ID
     */
    void triggerAssignmentAfterAudit(Long orderId);

    /**
     * 自动分配设计师（专业方向匹配 + 负载均衡）
     *
     * @param orderId 订单ID
     * @return 分配到的设计师用户ID，无可分配时返回 null
     */
    Long autoAssignDesigner(Long orderId);

    /**
     * 手动分配设计师（仅管理员；订单必须处于数据审核通过、待设计或设计中状态）
     *
     * @param orderId    订单ID
     * @param designerId 设计师用户ID
     */
    void manualAssignDesigner(Long orderId, Long designerId);

    /**
     * 查询可分配设计师列表（按专业方向过滤 + 负载排序）
     *
     * @param dto 查询条件
     * @return 匹配的设计师列表
     */
    List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto);
}
