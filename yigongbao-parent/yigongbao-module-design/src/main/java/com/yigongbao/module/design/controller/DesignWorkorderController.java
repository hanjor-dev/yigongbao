package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设计工单查询 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "设计工单", description = "设计工单列表查询与详情")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignWorkorderController {

    private final DesignWorkorderService designWorkorderService;

    /**
     * 分页查询设计工单列表
     */
    @Operation(summary = "分页查询设计工单列表")
    @RequirePermission(value = "design:View")
    @PostMapping("/list")
    public Result<IPage<DesignWorkorderListVO>> listWorkorders(@Validated @RequestBody DesignWorkorderQueryDTO queryDTO) {
        return Result.success(designWorkorderService.listWorkorders(queryDTO));
    }

    /**
     * 获取设计工单详情
     */
    @Operation(summary = "获取设计工单详情")
    @RequirePermission(value = "design:View")
    @GetMapping("/{orderId}")
    public Result<DesignWorkorderDetailVO> getWorkorderDetail(@PathVariable Long orderId) {
        return Result.success(designWorkorderService.getWorkorderDetail(orderId));
    }

    /**
     * 设计师开始设计
     * 仅分配给本人的订单，状态必须为待设计（PENDING_DESIGN）
     */
    @Operation(summary = "设计师开始设计")
    @RequirePermission(value = "design:StartDesign")
    @PostMapping("/{orderId}/start-design")
    public Result<Void> startDesign(@PathVariable Long orderId) {
        designWorkorderService.startDesign(orderId);
        return Result.success();
    }

    /**
     * 驳回后继续修改
     * POST /design/workorder/{orderId}/continue-design
     */
    @Operation(summary = "驳回后继续修改")
    @RequirePermission(value = "design:ContinueDesign")
    @PostMapping("/{orderId}/continue-design")
    public Result<Void> continueDesign(@PathVariable Long orderId) {
        designWorkorderService.continueDesign(orderId);
        return Result.success();
    }

    /**
     * 提交设计审核
     * POST /design/workorder/{orderId}/submit-design
     */
    @Operation(summary = "提交设计审核")
    @RequirePermission(value = "design:submitCheck")
    @PostMapping("/{orderId}/submit-design")
    public Result<Void> submitDesign(@PathVariable Long orderId) {
        designWorkorderService.submitDesign(orderId);
        return Result.success();
    }
}
