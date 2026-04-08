package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ExecuteModificationDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyPageQueryDTO;
import com.yigongbao.module.order.vo.modify.CanApplyModifyResult;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyDetailVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyListVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyVO;

import java.util.List;

/**
 * 订单修改申请 Service
 * 统一调用入口，所有模块均通过此接口使用修改申请功能
 *
 * @author hanjor
 * @date 2026-04-08
 */
public interface OrderModifyApplyService {

    /**
     * 判断订单是否可以发起修改申请
     *
     * @param orderId 订单ID
     * @return 判断结果（包含 canApply、allowedTypes、reason）
     */
    CanApplyModifyResult canApplyModify(Long orderId);

    /**
     * 发起修改申请
     *
     * @param orderId 订单ID
     * @param dto     申请参数
     * @return 申请 VO
     */
    ModifyApplyVO createApply(Long orderId, CreateModifyApplyDTO dto);

    /**
     * 撤回申请（逻辑删除，仅申请人可撤回待审核申请）
     *
     * @param applyId 申请ID
     */
    void withdrawApply(Long applyId);

    /**
     * 审核申请（同意/拒绝）
     *
     * @param applyId 申请ID
     * @param dto     审核参数
     */
    void auditApply(Long applyId, AuditModifyApplyDTO dto);

    /**
     * 执行订单修改（审核通过后调用）
     * 核心方法：统一修改入口，所有模块均通过此方法执行修改
     *
     * @param orderId 订单ID
     * @param applyId 修改申请ID
     * @param dto     修改字段参数
     */
    void executeModification(Long orderId, Long applyId, ExecuteModificationDTO dto);

    /**
     * 查询当前用户发起的申请列表（分页）
     *
     * @param dto 查询参数
     * @return 分页列表
     */
    IPage<ModifyApplyListVO> listMyApplies(ModifyApplyPageQueryDTO dto);

    /**
     * 查询待审核申请列表（管理员）
     *
     * @param dto 查询参数
     * @return 分页列表
     */
    IPage<ModifyApplyListVO> listPendingApplies(ModifyApplyPageQueryDTO dto);

    /**
     * 查询申请详情
     *
     * @param applyId 申请ID
     * @return 申请详情 VO
     */
    ModifyApplyDetailVO getApplyDetail(Long applyId);

    /**
     * 查询订单的所有申请记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    IPage<ModifyApplyListVO> listAppliesByOrder(Long orderId, ModifyApplyPageQueryDTO dto);

    /**
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto);

    /**
     * 校验字段是否在申请允许的修改范围内
     * 供设计模块等复用：校验 dto 中的字段是否在 applyId 申请的类型范围内
     *
     * @param applyId    申请ID
     * @param fieldNames 要修改的字段名列表
     */
    void validateFieldsInScope(Long applyId, List<String> fieldNames);
}
