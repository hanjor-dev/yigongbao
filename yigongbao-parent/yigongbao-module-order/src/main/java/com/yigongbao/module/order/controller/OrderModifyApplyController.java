package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
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
import org.springframework.web.bind.annotation.RestController;

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
@RequireSign
@Tag(name = "订单修改申请", description = "订单修改申请相关接口")
public class OrderModifyApplyController {

    private final OrderModifyApplyService orderModifyApplyService;

    // ==================== 订单维度接口 ====================

    @Operation(summary = "获取订单可申请的修改类型",
            description = "返回当前订单可申请的修改类型列表。allowedTypes 为空时表示不可申请；"
                    + "pendingApplyId 不为 null 时表示已有待审核申请（reason=PENDING_EXISTS）")
    @RequirePermission(value = "order:View")
    @GetMapping("/{orderId}/applicable-types")
    public Result<ApplicableModifyTypesVO> getApplicableTypes(@PathVariable Long orderId) {
        return Result.success(orderModifyApplyService.getApplicableTypes(orderId));
    }

    @Deprecated(since = "2026-05-20", forRemoval = true)
    @Operation(summary = "发起修改申请（已废弃，请使用直接修改接口）")
    @RequirePermission(value = "order:ApplyModify")
    @PostMapping("/{orderId}/apply")
    public Result<ModifyApplyVO> createApply(@PathVariable Long orderId,
            @Valid @RequestBody CreateModifyApplyDTO dto) {
        return Result.success(orderModifyApplyService.createApply(orderId, dto));
    }

    @Deprecated(since = "2026-05-20", forRemoval = true)
    @Operation(summary = "执行订单修改（已废弃，请使用直接修改接口）",
            description = "必须提供已审核通过（APPROVED 状态）的 applyId，否则报错。"
                    + "参数说明：\n"
                    + "① infoFields（14.1 基础信息）：差量列表，只传需要修改的字段。"
                    + "每个元素格式为 {\"field\":\"字段名\",\"value\":\"新值\"}，"
                    + "字段名须与 sys_config key=order.modify.field.config 中 \"14.1\".fields[].field 一致。\n"
                    + "② items（14.3 重建项目）：全量替换列表，列表即为最终状态。"
                    + "orderItemId 不为 null 时修改已有项目，null 时新增；不在列表内的旧项目自动删除。"
                    + "item 内 fields 字段名须与配置 \"14.3\".fields[].field 一致。\n"
                    + "③ imageDataFileIds / imageReportFileIds（14.2 影像文件）：全量替换文件ID列表，"
                    + "两者共用 14.2 申请类型控制，传 null 表示不修改该类别，传空列表表示清空。\n"
                    + "各子结构仅在申请类型白名单内时生效，白名单外的字段即使传入也会被忽略。")
    @RequirePermission(value = "order:Modify")
    @PutMapping("/execute/{applyId}")
    public Result<Void> executeModification(@PathVariable Long applyId,
            @Valid @RequestBody ExecuteModifyDTO dto) {
        orderModifyApplyService.executeModification(applyId, dto);
        return Result.success();
    }

    @Operation(summary = "直接修改订单（无需申请审核）",
            description = "根据订单当前阶段判断允许的修改类型：\n"
                    + "订单阶段（phase=10）：允许全部三种类型（14.1基础信息/14.2影像文件/14.3重建项目）\n"
                    + "设计阶段（phase=20）：仅允许重建项目（14.3）\n"
                    + "参数说明同 executeModification 接口")
    @RequirePermission(value = "order:Modify")
    @PutMapping("/{orderId}/direct")
    public Result<Void> directModify(@PathVariable Long orderId,
            @Valid @RequestBody ExecuteModifyDTO dto) {
        orderModifyApplyService.directModify(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "查询订单的修改申请记录列表（分页）")
    @RequirePermission(value = "order:View")
    @PostMapping("/{orderId}/applies")
    public Result<IPage<ModifyApplyListVO>> listAppliesByOrder(@PathVariable Long orderId,
            @RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listAppliesByOrder(orderId, dto));
    }

    @Operation(summary = "查询订单的修改留痕记录（分页）")
    @RequirePermission(value = "order:View")
    @PostMapping("/{orderId}/logs")
    public Result<IPage<ModificationLogVO>> listModificationLogs(@PathVariable Long orderId,
            @RequestBody ModificationLogPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listModificationLogs(orderId, dto));
    }

    // ==================== 申请维度接口 ====================

    @Deprecated(since = "2026-05-20", forRemoval = true)
    @Operation(summary = "撤回修改申请（已废弃）")
    @RequirePermission(value = "order:MyApplyWithdraw")
    @DeleteMapping("/apply/{applyId}")
    public Result<Void> withdrawApply(@PathVariable Long applyId) {
        orderModifyApplyService.withdrawApply(applyId);
        return Result.success();
    }

    @Deprecated(since = "2026-05-20", forRemoval = true)
    @Operation(summary = "审核修改申请（已废弃）")
    @RequirePermission(value = "order:Approve")
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
    @RequirePermission(value = "order:MyApplyView")
    @PostMapping("/apply/my")
    public Result<IPage<ModifyApplyListVO>> listMyApplies(@RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listMyApplies(dto));
    }

    @Operation(summary = "查询待审核申请列表（管理员）")
    @RequirePermission(value = "order:AuditView")
    @PostMapping("/apply/pending")
    public Result<IPage<ModifyApplyListVO>> listPendingApplies(
            @RequestBody ModifyApplyPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listPendingApplies(dto));
    }
}
