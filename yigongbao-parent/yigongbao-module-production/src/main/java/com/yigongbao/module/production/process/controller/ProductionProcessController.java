package com.yigongbao.module.production.process.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.dto.SubmitProcessQcDTO;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.process.vo.ProcessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "提交工序质检结果")
    @PostMapping("/{id}/submit-qc")
    public Result<Void> submitProcessQc(@PathVariable Long id, @Valid @RequestBody SubmitProcessQcDTO dto) {
        processService.submitProcessQc(id, dto);
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

    @Operation(summary = "获取工序列表")
    @GetMapping("/{recordId}/list")
    public Result<List<ProcessVO>> listProcesses(@PathVariable Long recordId) {
        return Result.success(processService.listProcesses(recordId));
    }

    @Operation(summary = "开始工序")
    @PostMapping("/{recordId}/start")
    public Result<Void> startProcess(@PathVariable Long recordId, @Valid @RequestBody StartProcessDTO dto) {
        processService.startProcess(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "完成工序")
    @PostMapping("/{recordId}/finish")
    public Result<Void> finishProcess(@PathVariable Long recordId, @RequestParam String processType) {
        processService.finishProcess(recordId, processType);
        return Result.success();
    }
}
