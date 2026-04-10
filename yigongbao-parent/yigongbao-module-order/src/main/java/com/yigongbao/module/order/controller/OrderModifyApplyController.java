package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyPageQueryDTO;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.vo.modify.ApplicableModifyTypesVO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyDetailVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyListVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订单修改申请 Controller
 * 统一入口，所有修改申请相关接口均在此处，路径前缀 /order/modify
 *
 * @author hanjor
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/order/modify")
@RequiredArgsConstructor
@Tag(name = "订单修改申请", description = "订单修改申请相关接口")
public class OrderModifyApplyController {

    private final OrderModifyApplyService orderModifyApplyService;

    // ==================== 订单维度接口 ====================

    @Operation(summary = "获取订单可申请的修改类型",
            description = "返回当前订单可申请的修改类型列表。allowedTypes 为空时表示不可申请；"
                    + "pendingApplyId 不为 null 时表示已有待审核申请（reason=PENDING_EXISTS）")
    @GetMapping("/{orderId}/applicable-types")
    public Result<ApplicableModifyTypesVO> getApplicableTypes(@PathVariable Long orderId) {
        return Result.success(orderModifyApplyService.getApplicableTypes(orderId));
    }

    @Operation(summary = "发起修改申请")
    @PostMapping("/{orderId}/apply")
    public Result<ModifyApplyVO> createApply(@PathVariable Long orderId,
            @Valid @RequestBody CreateModifyApplyDTO dto) {
        return Result.success(orderModifyApplyService.createApply(orderId, dto));
    }

    @Operation(summary = "执行订单修改（审核通过后调用）",
            description = "必须提供已审核通过（APPROVED 状态）的 applyId，否则报错。"
                    + "modifications 为 Map，只传需要修改的字段，后端根据申请类型白名单过滤并处理。")
    @PutMapping("/execute/{applyId}")
    public Result<Void> executeModification(@PathVariable Long applyId,
            @RequestBody Map<String, Object> modifications) {
        orderModifyApplyService.executeModification(applyId, modifications);
        return Result.success();
    }

    @Operation(summary = "查询订单的修改申请记录列表（分页）")
    @PostMapping("/{orderId}/applies")
    public Result<IPage<ModifyApplyListVO>> listAppliesByOrder(@PathVariable Long orderId,
            @RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listAppliesByOrder(orderId, dto));
    }

    @Operation(summary = "查询订单的修改留痕记录（分页）")
    @PostMapping("/{orderId}/logs")
    public Result<IPage<ModificationLogVO>> listModificationLogs(@PathVariable Long orderId,
            @RequestBody ModificationLogPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listModificationLogs(orderId, dto));
    }

    // ==================== 申请维度接口 ====================

    @Operation(summary = "撤回修改申请（仅申请人可撤回待审核申请）")
    @DeleteMapping("/apply/{applyId}")
    public Result<Void> withdrawApply(@PathVariable Long applyId) {
        orderModifyApplyService.withdrawApply(applyId);
        return Result.success();
    }

    @Operation(summary = "审核修改申请（同意/拒绝）")
    @PutMapping("/apply/{applyId}/audit")
    public Result<Void> auditApply(@PathVariable Long applyId,
            @Valid @RequestBody AuditModifyApplyDTO dto) {
        orderModifyApplyService.auditApply(applyId, dto);
        return Result.success();
    }

    @Operation(summary = "查询申请详情")
    @GetMapping("/apply/{applyId}")
    public Result<ModifyApplyDetailVO> getApplyDetail(@PathVariable Long applyId) {
        return Result.success(orderModifyApplyService.getApplyDetail(applyId));
    }

    // ==================== 查询类接口 ====================

    @Operation(summary = "查询我发起的申请列表（分页）")
    @PostMapping("/apply/my")
    public Result<IPage<ModifyApplyListVO>> listMyApplies(@RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listMyApplies(dto));
    }

    @Operation(summary = "查询待审核申请列表（管理员）")
    @PostMapping("/apply/pending")
    public Result<IPage<ModifyApplyListVO>> listPendingApplies(
            @RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listPendingApplies(dto));
    }
}
