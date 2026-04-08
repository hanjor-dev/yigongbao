package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyPageQueryDTO;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.vo.modify.CanApplyModifyResult;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单修改申请 Controller
 *
 * @author hanjor
 * @date 2026-04-08
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单修改申请", description = "订单修改申请相关接口")
public class OrderModifyApplyController {

    private final OrderModifyApplyService orderModifyApplyService;

    // ==================== 阶段判断 ====================

    @Operation(summary = "判断订单是否可发起修改申请")
    @GetMapping("/{id}/can-apply-modify")
    public Result<CanApplyModifyResult> canApplyModify(@PathVariable Long id) {
        return Result.success(orderModifyApplyService.canApplyModify(id));
    }

    // ==================== 申请管理 ====================

    @Operation(summary = "发起修改申请")
    @PostMapping("/{id}/modify-apply")
    public Result<ModifyApplyVO> createApply(@PathVariable Long id,
            @Valid @RequestBody CreateModifyApplyDTO dto) {
        return Result.success(orderModifyApplyService.createApply(id, dto));
    }

    @Operation(summary = "撤回修改申请")
    @DeleteMapping("/modify-apply/{id}")
    public Result<Void> withdrawApply(@PathVariable Long id) {
        orderModifyApplyService.withdrawApply(id);
        return Result.success();
    }

    @Operation(summary = "审核修改申请（同意/拒绝）")
    @PutMapping("/modify-apply/{id}/audit")
    public Result<Void> auditApply(@PathVariable Long id,
            @Valid @RequestBody AuditModifyApplyDTO dto) {
        orderModifyApplyService.auditApply(id, dto);
        return Result.success();
    }

    // ==================== 申请查询 ====================

    @Operation(summary = "查询我发起的申请列表")
    @PostMapping("/modify-apply/my")
    public Result<IPage<ModifyApplyListVO>> listMyApplies(@RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listMyApplies(dto));
    }

    @Operation(summary = "查询待审核申请列表（管理员）")
    @PostMapping("/modify-apply/pending")
    public Result<IPage<ModifyApplyListVO>> listPendingApplies(
            @RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listPendingApplies(dto));
    }

    @Operation(summary = "查询申请详情")
    @GetMapping("/modify-apply/{id}")
    public Result<ModifyApplyDetailVO> getApplyDetail(@PathVariable Long id) {
        return Result.success(orderModifyApplyService.getApplyDetail(id));
    }
}
