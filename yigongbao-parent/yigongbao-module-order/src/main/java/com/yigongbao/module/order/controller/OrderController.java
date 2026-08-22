package com.yigongbao.module.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.dto.draft.OrderDraftPageQueryDTO;
import com.yigongbao.module.order.dto.order.AssignDesignerDTO;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.dto.order.OrderCustomExportDTO;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.dto.workload.DesignerWorkloadExportDTO;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderDraftService;
import com.yigongbao.module.order.service.OrderExportService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.order.vo.draft.OrderDraftDetailVO;
import com.yigongbao.module.order.vo.draft.OrderDraftVO;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderExportFieldVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.order.vo.order.OrderStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 Controller
 *
 * @author hanjor
 * @date 2026-03-31
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关接口")
@RequireSign
public class OrderController {

    private final OrderDraftService orderDraftService;
    private final OrderMainService orderMainService;
    private final OrderExportService orderExportService;
    private final DesignerAssignmentService designerAssignmentService;

    // ==================== 草稿接口 ====================

    @Operation(summary = "分页查询我的草稿列表")
    @PostMapping("/draft/list")
    public Result<IPage<OrderDraftVO>> listDrafts(@Valid @RequestBody OrderDraftPageQueryDTO dto) {
        return Result.success(orderDraftService.listDrafts(dto));
    }

    @Operation(summary = "查询草稿详情")
    @GetMapping("/draft/{id}")
    public Result<OrderDraftDetailVO> getDraftDetail(@PathVariable Long id) {
        orderDraftService.validateDraftOwner(id, StpUtil.getLoginIdAsLong());
        return Result.success(orderDraftService.getDraftDetail(id));
    }

    @Operation(summary = "创建或更新草稿")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CREATE, operation = "创建或更新草稿")
    @PostMapping("/draft")
    public Result<Long> saveDraft(@Valid @RequestBody CreateOrderDraftDTO dto) {
        return Result.success(orderDraftService.saveDraft(dto));
    }

    @Operation(summary = "删除草稿")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.DELETE, operation = "删除草稿")
    @DeleteMapping("/draft/{id}")
    public Result<Void> removeDraft(@PathVariable Long id) {
        orderDraftService.removeDraft(id);
        return Result.success();
    }

    // ==================== 订单接口 ====================

    @Operation(summary = "直接创建订单（直提，不经过草稿）")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CREATE, operation = "创建订单")
    @PostMapping
    public Result<Long> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderMainService.createOrder(dto));
    }

    @Operation(summary = "分页查询订单列表")
    @PostMapping("/page")
    public Result<IPage<OrderListVO>> listOrders(@Valid @RequestBody OrderPageDTO dto) {
        return Result.success(orderMainService.listOrders(dto));
    }

    @Operation(summary = "统计订单数量")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderMainService.statistics());
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderMainService.getOrderDetail(id));
    }

    @Operation(summary = "提交订单")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.SUBMIT, operation = "提交订单")
    @PostMapping("/{id}/submit")
    public Result<Void> submitOrder(@PathVariable Long id) {
        orderMainService.submitOrder(id);
        return Result.success();
    }

    @Operation(summary = "撤回订单")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "撤回订单")
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdrawOrder(@PathVariable Long id) {
        orderMainService.withdrawOrder(id);
        return Result.success();
    }

    @Operation(summary = "审核通过")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.AUDIT, operation = "审核通过")
    @PostMapping("/{id}/audit-pass")
    public Result<Void> auditPass(@PathVariable Long id, @Valid @RequestBody AuditOrderDTO dto) {
        orderMainService.auditPass(id, dto);
        return Result.success();
    }

    @Operation(summary = "审核驳回")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.AUDIT, operation = "审核驳回")
    @PostMapping("/{id}/audit-reject")
    public Result<Void> auditReject(@PathVariable Long id, @Valid @RequestBody AuditOrderDTO dto) {
        orderMainService.auditReject(id, dto);
        return Result.success();
    }

    @Operation(summary = "重新提交订单")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "重新提交订单")
    @PostMapping("/{id}/resubmit")
    public Result<Void> resubmit(@PathVariable Long id, @Valid @RequestBody com.yigongbao.module.order.dto.order.ResubmitOrderDTO dto) {
        orderMainService.resubmit(id, dto.getVersion());
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CANCEL, operation = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, @Valid @RequestBody com.yigongbao.module.order.dto.order.CancelOrderDTO dto) {
        orderMainService.cancelOrder(id, dto.getVersion());
        return Result.success();
    }

    @Operation(summary = "手动完成订单")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "手动完成订单")
    @PostMapping("/{id}/manual-complete")
    public Result<Void> manualCompleteOrder(@PathVariable Long id, @Valid @RequestBody com.yigongbao.module.order.dto.order.ManualCompleteOrderDTO dto) {
        orderMainService.manualCompleteOrder(id, dto.getVersion());
        return Result.success();
    }

    @Operation(summary = "查询可执行的动作")
    @GetMapping("/{id}/actions")
    public Result<List<String>> listAvailableActions(@PathVariable Long id) {
        return Result.success(orderMainService.listAvailableActions(id));
    }

    @Operation(summary = "查询可分配设计师列表")
    @PostMapping("/designers/available")
    public Result<List<DesignerVO>> listAvailableDesigners(@RequestBody DesignerQueryDTO dto) {
        return Result.success(designerAssignmentService.listAvailableDesigners(dto));
    }

    @Operation(summary = "手动分配设计师（管理员）")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.ASSIGN, operation = "分配设计师")
    @PostMapping("/{id}/assign-designer")
    public Result<Void> assignDesigner(@PathVariable Long id, @Valid @RequestBody AssignDesignerDTO dto) {
        designerAssignmentService.manualAssignDesigner(id, dto.getDesignerId());
        return Result.success();
    }

    // ==================== 列配置接口 ====================

    @Operation(summary = "获取当前用户列配置")
    @GetMapping("/column-config")
    public Result<OrderColumnConfigVO> getColumnConfig() {
        return Result.success(orderMainService.getColumnConfig());
    }

    @Operation(summary = "保存用户列配置")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "保存列配置")
    @PutMapping("/column-config")
    public Result<Void> saveColumnConfig(@Valid @RequestBody OrderColumnConfigVO config) {
        orderMainService.saveColumnConfig(config);
        return Result.success();
    }

    @Operation(summary = "重置用户列配置（恢复系统默认）")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "重置列配置")
    @DeleteMapping("/column-config")
    public Result<Void> resetColumnConfig() {
        orderMainService.resetColumnConfig();
        return Result.success();
    }

    // ==================== 导出接口 ====================

    @Operation(summary = "导出订单列表（Excel）")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.EXPORT, operation = "导出订单")
    @PostMapping("/export")
    public void exportOrders(@RequestBody OrderExportQueryDTO dto, HttpServletResponse response) {
        orderExportService.exportOrders(dto, response);
    }

    @Operation(summary = "自定义字段导出订单（Excel）")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.EXPORT, operation = "自定义导出订单")
    @PostMapping("/export/custom")
    public void customExportOrders(@Valid @RequestBody OrderCustomExportDTO dto, HttpServletResponse response) {
        orderExportService.customExportOrders(dto, response);
    }

    @Operation(summary = "获取可导出字段列表")
    @GetMapping("/export/fields")
    public Result<List<OrderExportFieldVO>> getAvailableExportFields() {
        return Result.success(orderExportService.getAvailableExportFields());
    }
}
