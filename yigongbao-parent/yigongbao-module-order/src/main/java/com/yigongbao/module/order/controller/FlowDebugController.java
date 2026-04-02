package com.yigongbao.module.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.FlowStatusHistoryEntity;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import com.yigongbao.module.order.service.OrderMainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流转状态机 Debug 控制器
 * 提供状态机开发调试接口，仅开发阶段使用，禁止暴露到外网
 *
 * @author hanjor
 * @date 2026-04-02
 */
@RestController
@RequestMapping("/api/order/debug")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "【开发调试】流转状态机", description = "状态机开发调试接口，仅开发阶段使用")
public class FlowDebugController {

    private final FlowFacade flowFacade;
    private final FlowStatusHistoryService flowStatusHistoryService;
    private final OrderMainService orderMainService;

    /**
     * 预览状态转换结果（不落库）
     * 用于在执行动作前预览转换结果，不实际修改订单状态
     *
     * @param id 订单ID
     * @param actionCode 动作编码
     * @return 转换结果预览
     */
    @Operation(summary = "预览状态转换结果（不落库）")
    @GetMapping("/preview")
    public Result<TransitionResult> preview(
            @RequestParam Long id,
            @RequestParam String actionCode) {
        FlowActionEnum action = FlowActionEnum.getByCode(actionCode);
        if (action == null) {
            throw new BusinessException(400, "未知动作编码：" + actionCode);
        }
        Long currentUserId = getCurrentUserId();
        log.info("【Debug】预览状态转换，orderId={}, action={}", id, actionCode);
        TransitionResult result = flowFacade.executeFlow(
                id, action, FlowOperator.of(currentUserId, null));
        return Result.success(result);
    }

    /**
     * 执行任意流转动作（真实落库）
     * 用于开发阶段手动触发任意状态转换，测试状态机逻辑
     *
     * @param id 订单ID
     * @param actionCode 动作编码
     * @param remark 备注（可选）
     * @return 转换结果
     */
    @Operation(summary = "执行任意流转动作（真实落库）")
    @PostMapping("/execute")
    public Result<TransitionResult> execute(
            @RequestParam Long id,
            @RequestParam String actionCode,
            @RequestParam(required = false) String remark) {
        FlowActionEnum action = FlowActionEnum.getByCode(actionCode);
        if (action == null) {
            throw new BusinessException(400, "未知动作编码：" + actionCode);
        }
        Long currentUserId = getCurrentUserId();
        log.info("【Debug】执行流转动作，orderId={}, action={}, currentUserId={}",
                id, actionCode, currentUserId);
        TransitionResult result = flowFacade.executeFlow(
                id, action, new FlowOperator(currentUserId, null, remark));

        // 更新数据库
        OrderMainEntity entity = orderMainService.getById(id);
        if (entity == null) {
            throw new BusinessException(675, "订单不存在");
        }
        entity.setPhase(result.getTargetPhase());
        entity.setStatus(result.getFinalStatus());
        if (currentUserId != null) {
            entity.setCurrentHandlerId(currentUserId);
        }
        orderMainService.updateById(entity);
        log.info("【Debug】执行流转动作成功，orderId={}, phase={}, status={}",
                id, result.getTargetPhase(), result.getFinalStatus());
        return Result.success(result);
    }

    /**
     * 重置订单到指定阶段和状态
     * 用于开发阶段将订单重置到任意状态，方便测试各种场景
     *
     * @param id 订单ID
     * @param phase 目标阶段
     * @param status 目标状态
     * @return 重置结果
     */
    @Operation(summary = "重置订单到指定阶段和状态")
    @PostMapping("/reset")
    public Result<Void> reset(
            @RequestParam Long id,
            @RequestParam Integer phase,
            @RequestParam Integer status) {
        log.info("【Debug】重置订单状态，orderId={}, phase={}, status={}", id, phase, status);
        OrderMainEntity entity = orderMainService.getById(id);
        if (entity == null) {
            throw new BusinessException(675, "订单不存在");
        }
        entity.setPhase(phase);
        entity.setStatus(status);
        orderMainService.updateById(entity);
        log.info("【Debug】重置订单状态成功，orderId={}, newPhase={}, newStatus={}",
                id, phase, status);
        return Result.success();
    }

    /**
     * 查看订单的流转历史记录
     * 用于开发阶段查看状态变更轨迹
     *
     * @param id 订单ID
     * @return 历史记录列表
     */
    @Operation(summary = "查看订单的流转历史记录")
    @GetMapping("/history")
    public Result<List<FlowStatusHistoryEntity>> history(@RequestParam Long id) {
        log.info("【Debug】查询流转历史，orderId={}", id);
        List<FlowStatusHistoryEntity> historyList = flowStatusHistoryService.listByOrderId(id);
        return Result.success(historyList);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，可能未登录", e);
            return null;
        }
    }
}
