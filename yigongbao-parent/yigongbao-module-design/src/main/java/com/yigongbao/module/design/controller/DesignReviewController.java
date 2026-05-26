package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.service.DesignReviewService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设计审核 Controller
 *
 * @author hanjor
 * @date 2026-04-17
 */
@RestController
@RequestMapping("/design/review")
@RequiredArgsConstructor
@Tag(name = "设计审核")
public class DesignReviewController {

    private final DesignReviewService designReviewService;

    /**
     * 分页查询待审核工单列表
     * 固定 status=2040，前端无需传 status 参数
     */
    @PostMapping("/list")
    @RequirePermission(value = "design:ReviewView")
    @Operation(summary = "待审核工单列表")
    public Result<IPage<DesignWorkorderListVO>> listReviewWorkorders(
            @RequestBody DesignWorkorderQueryDTO queryDTO) {
        return Result.success(designReviewService.listReviewWorkorders(queryDTO));
    }

    /**
     * 获取审核详情（工单详情 + 审核历史）
     */
    @GetMapping("/{orderId}")
    @RequirePermission(value = "design:ReviewView")
    @Operation(summary = "审核详情")
    public Result<DesignReviewDetailVO> getReviewDetail(@PathVariable Long orderId) {
        return Result.success(designReviewService.getReviewDetail(orderId));
    }

    /**
     * 审核通过
     * 状态流转：2040 → 3010（需实体交付）或 7010（不需实体交付）
     */
    @PostMapping("/{orderId}/pass")
    @RequirePermission(value = "design:Approve")
    @Operation(summary = "审核通过")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.AUDIT, operation = "设计审核通过")
    public Result<Void> reviewPass(@PathVariable Long orderId,
                                   @RequestBody(required = false) ReviewPassDTO dto) {
        designReviewService.reviewPass(orderId, dto != null ? dto : new ReviewPassDTO());
        return Result.success();
    }

    /**
     * 审核驳回
     * 状态流转：2040 → 2060
     */
    @PostMapping("/{orderId}/reject")
    @RequirePermission(value = "design:Reject")
    @Operation(summary = "审核驳回")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.AUDIT, operation = "设计审核驳回")
    public Result<Void> reviewReject(@PathVariable Long orderId,
                                     @Valid @RequestBody ReviewRejectDTO dto) {
        designReviewService.reviewReject(orderId, dto);
        return Result.success();
    }
}
