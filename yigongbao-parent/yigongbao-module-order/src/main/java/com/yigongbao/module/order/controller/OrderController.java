package com.yigongbao.module.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.service.OrderDraftService;
import com.yigongbao.module.order.service.OrderExportService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.draft.OrderDraftDetailVO;
import com.yigongbao.module.order.vo.draft.OrderDraftVO;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
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
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关接口")
public class OrderController {

    private final OrderDraftService orderDraftService;
    private final OrderMainService orderMainService;
    private final OrderExportService orderExportService;

    // ==================== 草稿接口 ====================

    @Operation(summary = "分页查询我的草稿列表")
    @GetMapping("/draft/list")
    public Result<IPage<OrderDraftVO>> listDrafts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer status) {
        return Result.success(orderDraftService.listDrafts(pageNum, pageSize, hospitalId, status));
    }

    @Operation(summary = "查询草稿详情")
    @GetMapping("/draft/{id}")
    public Result<OrderDraftDetailVO> getDraftDetail(@PathVariable Long id) {
        orderDraftService.validateDraftOwner(id, StpUtil.getLoginIdAsLong());
        return Result.success(orderDraftService.getDraftDetail(id));
    }

    @Operation(summary = "创建或更新草稿")
    @PostMapping("/draft")
    public Result<Long> saveDraft(@Valid @RequestBody CreateOrderDraftDTO dto) {
        return Result.success(orderDraftService.saveDraft(dto));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/draft/{id}")
    public Result<Void> removeDraft(@PathVariable Long id) {
        orderDraftService.removeDraft(id);
        return Result.success();
    }

    @Operation(summary = "提交草稿，转为正式订单")
    @PostMapping("/draft/{id}/submit")
    public Result<Long> submitDraft(@PathVariable Long id) {
        return Result.success(orderDraftService.submitDraft(id));
    }

    // ==================== 订单接口 ====================

    @Operation(summary = "直接创建订单（直提，不经过草稿）")
    @PostMapping
    public Result<Long> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderMainService.createOrder(dto));
    }

    @Operation(summary = "分页查询订单列表")
    @PostMapping("/page")
    public Result<IPage<OrderListVO>> listOrders(@RequestBody OrderPageDTO dto) {
        return Result.success(orderMainService.listOrders(dto));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderMainService.getOrderDetail(id));
    }

    @Operation(summary = "提交订单")
    @PostMapping("/{id}/submit")
    public Result<Void> submitOrder(@PathVariable Long id) {
        orderMainService.submitOrder(id);
        return Result.success();
    }

    @Operation(summary = "撤回订单")
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdrawOrder(@PathVariable Long id) {
        orderMainService.withdrawOrder(id);
        return Result.success();
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/audit-pass")
    public Result<Void> auditPass(@PathVariable Long id, @Valid @RequestBody AuditOrderDTO dto) {
        orderMainService.auditPass(id, dto);
        return Result.success();
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/{id}/audit-reject")
    public Result<Void> auditReject(@PathVariable Long id, @Valid @RequestBody AuditOrderDTO dto) {
        orderMainService.auditReject(id, dto);
        return Result.success();
    }

    @Operation(summary = "查询可执行的动作")
    @GetMapping("/{id}/actions")
    public Result<List<String>> listAvailableActions(@PathVariable Long id) {
        return Result.success(orderMainService.listAvailableActions(id));
    }

    @Operation(summary = "删除订单（仅草稿状态）")
    @DeleteMapping("/{id}")
    public Result<Void> removeOrder(@PathVariable Long id) {
        orderMainService.removeOrder(id);
        return Result.success();
    }

    // ==================== 列配置接口 ====================

    @Operation(summary = "获取当前用户列配置")
    @GetMapping("/column-config")
    public Result<OrderColumnConfigVO> getColumnConfig() {
        return Result.success(orderMainService.getColumnConfig());
    }

    @Operation(summary = "保存用户列配置")
    @PutMapping("/column-config")
    public Result<Void> saveColumnConfig(@RequestBody OrderColumnConfigVO config) {
        orderMainService.saveColumnConfig(config);
        return Result.success();
    }

    @Operation(summary = "重置用户列配置（恢复系统默认）")
    @DeleteMapping("/column-config")
    public Result<Void> resetColumnConfig() {
        orderMainService.resetColumnConfig();
        return Result.success();
    }

    // ==================== 导出接口 ====================

    @Operation(summary = "导出订单列表（Excel）")
    @PostMapping("/export")
    public void exportOrders(@RequestBody OrderExportQueryDTO dto, HttpServletResponse response) {
        orderExportService.exportOrders(dto, response);
    }
}
