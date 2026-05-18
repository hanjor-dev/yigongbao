package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
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
    @RequirePermission(value = "design:View")
    @GetMapping("/{orderId}/package/{packageId}/print-info/options")
    public Result<PrintInfoOptionsVO> getOptions(@PathVariable Long orderId,
                                                  @PathVariable Long packageId) {
        return Result.success(printInfoService.getOptions(orderId, packageId));
    }

    /**
     * 查询数据包打印信息列表
     */
    @Operation(summary = "查询打印信息列表")
    @RequirePermission(value = "design:PrintInfo")
    @GetMapping("/{orderId}/package/{packageId}/print-info")
    public Result<PrintInfoListVO> listPrintInfo(@PathVariable Long orderId,
                                                @PathVariable Long packageId) {
        return Result.success(printInfoService.listPrintInfo(orderId, packageId));
    }

    /**
     * 保存打印信息（整包替换，空列表=清空）
     */
    @Operation(summary = "保存打印信息（整包替换）")
    @RequirePermission(value = "design:PrintInfo")
    @PostMapping("/{orderId}/package/{packageId}/print-info")
    public Result<Void> savePrintInfo(@PathVariable Long orderId,
                                      @PathVariable Long packageId,
                                      @Validated @RequestBody SavePrintInfoDTO dto) {
        printInfoService.savePrintInfo(orderId, packageId, dto);
        return Result.success();
    }

    /**
     * 删除单条打印信息
     */
    @Operation(summary = "删除单条打印信息")
    @RequirePermission(value = "design:PrintInfo")
    @DeleteMapping("/{orderId}/package/{packageId}/print-info/{printInfoId}")
    public Result<Void> deletePrintInfo(@PathVariable Long orderId,
                                         @PathVariable Long packageId,
                                         @PathVariable Long printInfoId) {
        printInfoService.deletePrintInfo(orderId, packageId, printInfoId);
        return Result.success();
    }
}
