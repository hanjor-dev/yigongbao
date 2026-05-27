package com.yigongbao.module.production.process.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工序操作管理
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "工序操作管理")
@RestController
@RequestMapping("/production/process")
@RequiredArgsConstructor
public class ProductionProcessController {

    private final IProductionProcessService processService;

    @Operation(summary = "填写工序信息")
    @PutMapping("/{id}/fill")
    public Result<Void> fillProcess(@PathVariable Long id, @Valid @RequestBody FillProcessDTO dto) {
        processService.fillProcess(id, dto);
        return Result.success();
    }

    @Operation(summary = "工序流转")
    @PostMapping("/{recordId}/transfer")
    public Result<Void> transferToNext(@PathVariable Long recordId,
                                       @RequestParam String fromProcess,
                                       @RequestParam String toProcess) {
        processService.transferToNext(recordId, fromProcess, toProcess);
        return Result.success();
    }

    @Operation(summary = "打印失败处理")
    @PostMapping("/{recordId}/print-failure")
    public Result<Void> handlePrintFailure(@PathVariable Long recordId,
                                           @RequestParam String failureReason,
                                           @RequestParam boolean recreate) {
        processService.handlePrintFailure(recordId, failureReason, recreate);
        return Result.success();
    }

    @Operation(summary = "打印检验不合格处理")
    @PostMapping("/{recordId}/print-inspection-fail")
    public Result<Void> handlePrintInspectionFail(@PathVariable Long recordId,
                                                  @RequestParam String failureReason,
                                                  @RequestParam boolean recreate) {
        processService.handlePrintInspectionFail(recordId, failureReason, recreate);
        return Result.success();
    }
}
