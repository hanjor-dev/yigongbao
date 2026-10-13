package com.yigongbao.module.design.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.service.DesignPrintInfoService;
import com.yigongbao.module.design.vo.PrintInfoListVO;
import com.yigongbao.module.design.vo.PrintInfoOptionsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 打印信息管理 Controller
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "打印信息管理", description = "设计阶段打印产品信息管理")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignPrintInfoController {

    private final DesignPrintInfoService printInfoService;

    /**
     * 获取打印信息选项数据（产品树、材质、颜色）以及包级已保存回显字段
     */
    @Operation(summary = "获取打印信息选项")
    @GetMapping("/{orderId}/package/{packageId}/print-info/options")
    public Result<PrintInfoOptionsVO> getOptions(@PathVariable Long orderId,
                                                  @PathVariable Long packageId,
                                                  @RequestParam(required = false) Long batchId) {
        return Result.success(batchId == null
                ? printInfoService.getOptions(orderId, packageId)
                : printInfoService.getOptions(orderId, packageId, batchId));
    }

    /**
     * 查询数据包打印信息列表
     */
    @Operation(summary = "查询打印信息列表")
    @GetMapping("/{orderId}/package/{packageId}/print-info")
    public Result<PrintInfoListVO> listPrintInfo(@PathVariable Long orderId,
                                                @PathVariable Long packageId,
                                                @RequestParam(required = false) Long batchId) {
        return Result.success(batchId == null
                ? printInfoService.listPrintInfo(orderId, packageId)
                : printInfoService.listPrintInfo(orderId, packageId, batchId));
    }

    /**
     * 保存打印信息（整包替换，空列表=清空）
     */
    @Operation(summary = "保存打印信息（整包替换）")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPDATE, operation = "保存打印信息")
    @PostMapping("/{orderId}/package/{packageId}/print-info")
    public Result<Void> savePrintInfo(@PathVariable Long orderId,
                                      @PathVariable Long packageId,
                                      @RequestParam(required = false) Long batchId,
                                      @Validated @RequestBody SavePrintInfoDTO dto) {
        if (batchId == null) {
            printInfoService.savePrintInfo(orderId, packageId, dto);
        } else {
            printInfoService.savePrintInfo(orderId, packageId, batchId, dto);
        }
        return Result.success();
    }

    /**
     * 删除单条打印信息
     */
    @Operation(summary = "删除单条打印信息")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.DELETE, operation = "删除打印信息")
    @DeleteMapping("/{orderId}/package/{packageId}/print-info/{printInfoId}")
    public Result<Void> deletePrintInfo(@PathVariable Long orderId,
                                         @PathVariable Long packageId,
                                         @PathVariable Long printInfoId,
                                         @RequestParam(required = false) Long batchId) {
        if (batchId == null) {
            printInfoService.deletePrintInfo(orderId, packageId, printInfoId);
        } else {
            printInfoService.deletePrintInfo(orderId, packageId, batchId, printInfoId);
        }
        return Result.success();
    }
}
