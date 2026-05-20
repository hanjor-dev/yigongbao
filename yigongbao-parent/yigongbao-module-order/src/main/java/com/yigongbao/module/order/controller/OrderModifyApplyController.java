package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单修改 Controller
 * 提供直接修改订单和查询修改留痕功能
 *
 * @author hanjor
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/order/modify")
@RequiredArgsConstructor
@RequireSign
@Tag(name = "订单修改", description = "订单直接修改和修改留痕查询")
public class OrderModifyApplyController {

    private final OrderModifyApplyService orderModifyApplyService;

    @Operation(summary = "直接修改订单（无需申请审核）",
            description = "根据订单当前阶段判断允许的修改类型：\n"
                    + "订单阶段（phase=10）：允许全部三种类型（14.1基础信息/14.2影像文件/14.3重建项目）\n"
                    + "设计阶段（phase=20）：仅允许重建项目（14.3）\n"
                    + "参数说明：\n"
                    + "① infoFields（14.1 基础信息）：差量列表，只传需要修改的字段\n"
                    + "② items（14.3 重建项目）：全量替换列表\n"
                    + "③ imageDataFileIds / imageReportFileIds（14.2 影像文件）：全量替换文件ID列表")
    @RequirePermission(value = "order:Modify")
    @PutMapping("/{orderId}/direct")
    public Result<Void> directModify(@PathVariable Long orderId,
            @Valid @RequestBody ExecuteModifyDTO dto) {
        orderModifyApplyService.directModify(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "查询订单的修改留痕记录（分页）")
    @RequirePermission(value = "order:View")
    @PostMapping("/{orderId}/logs")
    public Result<IPage<ModificationLogVO>> listModificationLogs(@PathVariable Long orderId,
            @RequestBody ModificationLogPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listModificationLogs(orderId, dto));
    }
}
