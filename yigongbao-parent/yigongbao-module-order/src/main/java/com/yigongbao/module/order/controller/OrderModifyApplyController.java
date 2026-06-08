package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.apply.ApplyListQueryDTO;
import com.yigongbao.module.order.dto.apply.AuditApplyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.service.OrderModifyFullService;
import com.yigongbao.module.order.vo.apply.ApplyDetailVO;
import com.yigongbao.module.order.vo.apply.ApplyListItemVO;
import com.yigongbao.module.order.vo.apply.SubmitApplyResultVO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单修改 Controller
 * 提供全量修改订单和查询修改留痕功能
 *
 * @author hanjor
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/order/modify")
@RequiredArgsConstructor
@RequireSign
@Tag(name = "订单修改", description = "订单全量修改和修改留痕查询")
public class OrderModifyApplyController {

    private final OrderModifyApplyService orderModifyApplyService;
    private final OrderModifyFullService orderModifyFullService;

    @Operation(summary = "全量修改订单",
            description = "前端传入完整订单数据，后端自动判断变更内容并应用。")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "全量修改订单")
    @PutMapping("/{orderId}/full")
    public Result<Void> modifyOrderFull(@PathVariable Long orderId,
            @Valid @RequestBody OrderModifyFullDTO dto) {
        orderModifyFullService.modifyOrderFull(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "全量修改订单（v2-带时间窗口检查）",
            description = "前端传入完整订单数据，后端检查时间窗口：\n"
                    + "在窗口内：直接修改并返回成功（code=200）\n"
                    + "超过窗口：返回错误码739，提示需要提交申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "全量修改订单（v2）")
    @PutMapping("/{orderId}/full-v2")
    public Result<Void> modifyOrderFullV2(@PathVariable Long orderId,
            @Valid @RequestBody OrderModifyFullDTO dto) {
        orderModifyApplyService.modifyOrderFullV2(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "查询订单的修改留痕记录（分页）")
    @PostMapping("/{orderId}/logs")
    public Result<IPage<ModificationLogVO>> listModificationLogs(@PathVariable Long orderId,
            @RequestBody ModificationLogPageQueryDTO dto) {
        return Result.success(orderModifyApplyService.listModificationLogs(orderId, dto));
    }

    @Operation(summary = "提交修改申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CREATE, operation = "提交修改申请")
    @PostMapping("/{orderId}/apply")
    public Result<SubmitApplyResultVO> submitApply(@PathVariable Long orderId,
            @Valid @RequestBody OrderModifyFullDTO dto) {
        Long applyId = orderModifyApplyService.submitApply(orderId, dto);
        OrderModificationApplyEntity apply = orderModifyApplyService.getApplyEntityById(applyId);
        SubmitApplyResultVO vo = new SubmitApplyResultVO();
        vo.setApplyId(applyId);
        vo.setExpireTime(apply.getExpireTime());
        return Result.success(vo);
    }

    @Operation(summary = "查询修改申请列表")
    @PostMapping("/apply/list")
    public Result<IPage<ApplyListItemVO>> listApplies(@Valid @RequestBody ApplyListQueryDTO dto) {
        return Result.success(orderModifyApplyService.listApplies(dto));
    }

    @Operation(summary = "查询申请详情")
    @GetMapping("/apply/{applyId}")
    public Result<ApplyDetailVO> getApplyDetail(@PathVariable Long applyId) {
        return Result.success(orderModifyApplyService.getApplyDetail(applyId));
    }

    @Operation(summary = "审核修改申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.UPDATE, operation = "审核修改申请")
    @PutMapping("/apply/{applyId}/audit")
    public Result<Void> auditApply(@PathVariable Long applyId,
            @Valid @RequestBody AuditApplyDTO dto) {
        orderModifyApplyService.auditApply(applyId, dto);
        return Result.success();
    }

    @Operation(summary = "查询我的申请记录")
    @PostMapping("/apply/my-list")
    public Result<IPage<ApplyListItemVO>> myListApplies(@Valid @RequestBody ApplyListQueryDTO dto) {
        return Result.success(orderModifyApplyService.myListApplies(dto));
    }
}
