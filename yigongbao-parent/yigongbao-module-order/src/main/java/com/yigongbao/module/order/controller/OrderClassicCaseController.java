package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单经典案例Controller
 * <p>
 * 提供经典案例的标记、查询等功能。
 * 经典案例是已完成订单中具有典型意义和参考价值的案例，
 * 标记后所有相关数据和文件将被保护，不允许修改或删除。
 * </p>
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Tag(name = "经典案例管理", description = "订单经典案例的标记、查询和管理")
@RestController
@RequestMapping("/order/classic-case")
@RequiredArgsConstructor
public class OrderClassicCaseController {

    private final IOrderClassicCaseService classicCaseService;

    /**
     * 标记订单为经典案例
     * <p>
     * 将已完成的订单标记为经典案例。标记后该订单的所有数据和文件将被保护，
     * 不允许进行任何修改、删除等操作。文件会自动迁移到经典案例专用目录。
     * </p>
     *
     * @param dto 标记请求参数（包含订单ID和备注）
     * @return 操作结果
     */
    @Operation(summary = "标记订单为经典案例", description = "将已完成的订单标记为经典案例，标记后数据将被保护")
    @OperationLog(module = "经典案例管理", businessType = OperationTypeEnum.UPDATE,
                  operation = "标记经典案例", logParams = true)
    @PostMapping("/mark")
    public Result<Void> markAsClassicCase(@RequestBody @Validated MarkClassicCaseDTO dto) {
        classicCaseService.markAsClassicCase(dto);
        return Result.success();
    }

    /**
     * 分页查询经典案例列表
     * <p>
     * 支持多条件筛选：订单编号、患者姓名、医院、标记时间范围。
     * 结果按经典案例标记时间倒序排列。
     * </p>
     *
     * @param dto 查询条件（包含分页参数和筛选条件）
     * @return 经典案例分页列表
     */
    @Operation(summary = "查询经典案例列表", description = "分页查询经典案例，支持多条件筛选")
    @PostMapping("/list")
    public Result<IPage<ClassicCaseVO>> list(@RequestBody ClassicCaseQueryDTO dto) {
        return Result.success(classicCaseService.listClassicCases(dto));
    }

    /**
     * 查询经典案例详情
     * <p>
     * 根据订单ID查询经典案例的详细信息。
     * 如果订单不存在或未标记为经典案例，则返回错误。
     * </p>
     *
     * @param orderId 订单ID
     * @return 经典案例详情
     */
    @Operation(summary = "查询经典案例详情", description = "根据订单ID查询经典案例的详细信息")
    @GetMapping("/{orderId}")
    public Result<ClassicCaseVO> detail(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        return Result.success(classicCaseService.getClassicCaseDetail(orderId));
    }
}
