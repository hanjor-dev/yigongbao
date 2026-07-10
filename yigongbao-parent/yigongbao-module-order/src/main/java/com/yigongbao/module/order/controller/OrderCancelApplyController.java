package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.order.AuditCancelApplyDTO;
import com.yigongbao.module.order.dto.order.CancelOrderApplyDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单取消申请管理 Controller
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@RestController
@RequestMapping("/order/cancel-apply")
@RequiredArgsConstructor
@Tag(name = "订单取消申请管理")
@RequireSign
public class OrderCancelApplyController {

    private final OrderCancelApplyService cancelApplyService;

    /**
     * 提交取消申请
     */
    @PostMapping
    @Operation(summary = "提交取消申请", description = "订单创建人或设计师可提交取消申请，订单需≥设计阶段且无待审核申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CREATE, operation = "提交取消申请")
    public Result<Long> submitCancelApply(@Valid @RequestBody CancelOrderApplyDTO dto) {
        Long applyId = cancelApplyService.submitCancelApply(dto);
        return Result.success(applyId);
    }

    /**
     * 审核取消申请
     */
    @PostMapping("/{applyId}/audit")
    @Operation(summary = "审核取消申请", description = "设计管理员审核取消申请，通过则执行取消流程，驳回则清除待审核标记")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.AUDIT, operation = "审核取消申请")
    public Result<Void> auditCancelApply(
            @Parameter(description = "申请ID") @PathVariable Long applyId,
            @Valid @RequestBody AuditCancelApplyDTO dto) {
        cancelApplyService.auditCancelApply(applyId, dto);
        return Result.success();
    }

    /**
     * 查询取消申请详情
     */
    @GetMapping("/{applyId}")
    @Operation(summary = "查询取消申请详情", description = "根据申请ID查询取消申请的详细信息")
    public Result<CancelApplyVO> getCancelApplyDetail(
            @Parameter(description = "申请ID") @PathVariable Long applyId) {
        CancelApplyVO vo = cancelApplyService.getCancelApplyDetail(applyId);
        return Result.success(vo);
    }

    /**
     * 分页查询待审核的取消申请列表（设计管理员使用）
     */
    @PostMapping("/pending/list")
    @Operation(summary = "查询待审核申请列表", description = "设计管理员查询所有待审核的取消申请")
    public Result<IPage<CancelApplyVO>> listPendingApplies(@Valid @RequestBody OrderPageDTO dto) {
        IPage<CancelApplyVO> page = cancelApplyService.listPendingApplies(dto);
        return Result.success(page);
    }

    /**
     * 分页查询当前用户的取消申请列表（我的申请）
     */
    @PostMapping("/my-applies")
    @Operation(summary = "查询我的申请列表", description = "查询当前用户提交的所有取消申请")
    public Result<IPage<CancelApplyVO>> listMyApplies(@Valid @RequestBody OrderPageDTO dto) {
        IPage<CancelApplyVO> page = cancelApplyService.listMyApplies(dto);
        return Result.success(page);
    }

    /**
     * 查询订单的取消申请历史记录
     */
    @GetMapping("/order/{orderId}/history")
    @Operation(summary = "查询订单申请历史", description = "查询指定订单的所有取消申请记录（按时间倒序）")
    public Result<List<CancelApplyVO>> getCancelApplyHistory(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        List<CancelApplyVO> history = cancelApplyService.getCancelApplyHistory(orderId);
        return Result.success(history);
    }
}
