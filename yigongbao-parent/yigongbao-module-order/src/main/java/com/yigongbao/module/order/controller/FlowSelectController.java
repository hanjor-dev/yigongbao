package com.yigongbao.module.order.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.service.FlowStatusColorResolver;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.common.vo.SelectTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 流转阶段/状态下拉 Controller
 * 提供 FlowPhaseEnum 和 FlowStatusEnum 枚举值的下拉列表接口，供前端状态筛选使用
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "流转下拉选项", description = "订单阶段和状态下拉列表接口")
@RestController
@RequestMapping("/flow/select")
@RequireSign
@RequiredArgsConstructor
public class FlowSelectController {

    private static final Set<FlowStatusEnum> HIDDEN_SELECT_STATUSES = EnumSet.of(
            FlowStatusEnum.DRAFT,
            FlowStatusEnum.DATA_AUDIT_PASSED,
            FlowStatusEnum.DATA_AUDIT_REJECTED,
            FlowStatusEnum.QC_PASSED,
            FlowStatusEnum.QC_FAILED,
            FlowStatusEnum.REWORK);

    private final FlowStatusColorResolver flowStatusColorResolver;

    /**
     * 获取流转阶段下拉列表
     * 返回 FlowPhaseEnum 所有枚举值，供前端阶段筛选使用
     *
     * @return 阶段列表（name=中文名称，value=阶段数值）
     */
    @Operation(summary = "获取流转阶段下拉列表")
    @GetMapping("/phases")
    public Result<List<SelectTreeVO>> listPhases() {
        List<SelectTreeVO> result = new ArrayList<>();
        for (FlowPhaseEnum phase : FlowPhaseEnum.values()) {
            SelectTreeVO vo = new SelectTreeVO();
            vo.setName(phase.getName());
            vo.setValue(String.valueOf(phase.getValue()));
            result.add(vo);
        }
        return Result.success(result);
    }

    /**
     * 获取流转状态下拉列表
     * 返回 FlowStatusEnum 枚举值，可通过 phase 参数过滤指定阶段的状态
     *
     * @param phase 阶段值（可选，传入则只返回该阶段的状态；不传则返回全部状态）
     * @return 状态列表（name=中文名称，value=状态数值，show=是否展示在前端筛选项中）
     */
    @Operation(summary = "获取流转状态下拉列表")
    @GetMapping("/statuses")
    public Result<List<SelectTreeVO>> listStatuses(
            @RequestParam(required = false) Integer phase) {
        FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
        List<SelectTreeVO> result = new ArrayList<>();
        for (FlowStatusEnum status : FlowStatusEnum.values()) {
            // 传了 phase 时只返回属于该阶段的状态
            if (phaseEnum != null && !status.belongsTo(phaseEnum)) {
                continue;
            }
            SelectTreeVO vo = new SelectTreeVO();
            vo.setName(status.getName());
            vo.setValue(String.valueOf(status.getValue()));
            vo.setColor(flowStatusColorResolver.getColor(status.getValue()));
            vo.setShow(!HIDDEN_SELECT_STATUSES.contains(status));
            result.add(vo);
        }
        return Result.success(result);
    }
}
