package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.apply.ApplyListQueryDTO;
import com.yigongbao.module.order.dto.apply.AuditApplyDTO;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.vo.apply.ApplyDetailVO;
import com.yigongbao.module.order.vo.apply.ApplyListItemVO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;

/**
 * 订单修改 Service
 * 提供全量修改订单和查询修改留痕功能
 *
 * @author hanjor
 * @date 2026-04-08
 */
public interface OrderModifyApplyService {

    /**
     * 提交订单修改申请（订单超时或设计阶段使用）
     *
     * @param orderId 订单ID
     * @param dto     完整订单修改数据
     * @return 申请ID
     */
    Long submitApply(Long orderId, OrderModifyFullDTO dto);

    /**
     * 审核修改申请
     *
     * @param applyId 申请ID
     * @param dto     审核结果
     */
    void auditApply(Long applyId, AuditApplyDTO dto);

    /**
     * 查询修改申请列表（设计管理员）
     *
     * @param dto 查询条件
     * @return 分页列表
     */
    IPage<ApplyListItemVO> listApplies(ApplyListQueryDTO dto);

    /**
     * 查询修改申请详情
     *
     * @param applyId 申请ID
     * @return 申请详情
     */
    ApplyDetailVO getApplyDetail(Long applyId);

    /**
     * 查询我的修改申请列表（业务员）
     *
     * @param dto 查询条件
     * @return 分页列表
     */
    IPage<ApplyListItemVO> myListApplies(ApplyListQueryDTO dto);

    /**
     * 根据ID查询申请实体（内部使用）
     *
     * @param applyId 申请ID
     * @return 申请实体
     */
    OrderModificationApplyEntity getApplyEntityById(Long applyId);

    /**
     * 全量修改订单（按角色、阶段和时间窗口判断是否需要申请）
     *
     * @param orderId 订单ID
     * @param dto     修改数据
     */
    Integer modifyOrderFullV2(Long orderId, OrderModifyFullDTO dto);

    /**
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto);

    /**
     * 检查订单是否存在待审核的修改申请
     *
     * @param orderId 订单ID
     * @return true=存在待审核申请, false=不存在
     */
    boolean hasPendingApply(Long orderId);

    /**
     * 实时查询当前用户是否可以打开订单修改页面。
     *
     * @param orderId 订单ID
     * @return true=允许打开，false=不允许打开
     */
    boolean canApply(Long orderId);
}
