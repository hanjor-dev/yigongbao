package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.dto.order.AuditCancelApplyDTO;
import com.yigongbao.module.order.dto.order.CancelOrderApplyDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.vo.order.CancelApplyVO;

import java.util.List;

/**
 * 订单取消申请 Service
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
public interface OrderCancelApplyService extends IService<OrderCancelApplyEntity> {

    /**
     * 提交取消申请
     * <p>
     * 业务规则：
     * - 订单存在且未取消
     * - 订单阶段≥20（设计阶段及之后）
     * - 无待审核的取消申请
     * - 仅订单创建人或设计师可提交
     *
     * @param dto 取消申请参数
     * @return 申请ID
     */
    Long submitCancelApply(CancelOrderApplyDTO dto);

    /**
     * 审核取消申请
     * <p>
     * 业务规则：
     * - 仅设计管理员可审核
     * - 申请状态为待审核
     * - 审核通过：执行订单取消流程
     * - 审核驳回：清除订单待审核标记
     *
     * @param applyId 申请ID
     * @param dto 审核参数
     */
    void auditCancelApply(Long applyId, AuditCancelApplyDTO dto);

    /**
     * 查询取消申请详情
     *
     * @param applyId 申请ID
     * @return 申请详情
     */
    CancelApplyVO getCancelApplyDetail(Long applyId);

    /**
     * 分页查询待审核的取消申请列表（设计管理员使用）
     *
     * @param dto 分页参数
     * @return 待审核申请列表
     */
    IPage<CancelApplyVO> listPendingApplies(OrderPageDTO dto);

    /**
     * 检查订单是否有待审核的取消申请
     *
     * @param orderId 订单ID
     * @return true=有待审核申请，false=无
     */
    boolean hasPendingCancelApply(Long orderId);

    /**
     * 分页查询当前用户的取消申请列表（我的申请）
     *
     * @param dto 分页参数
     * @return 我的申请列表
     */
    IPage<CancelApplyVO> listMyApplies(OrderPageDTO dto);

    /**
     * 查询订单的取消申请历史记录
     *
     * @param orderId 订单ID
     * @return 历史记录列表（按时间倒序）
     */
    List<CancelApplyVO> getCancelApplyHistory(Long orderId);
}
