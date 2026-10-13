package com.yigongbao.module.production.process.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
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

    @Operation(summary = "获取工序列表")
    @GetMapping("/{recordId}/list")
    public Result<List<ProcessVO>> listProcesses(@PathVariable Long recordId) {
        return Result.success(processService.listProcesses(recordId));
    }

    @Operation(summary = "开始工序")
    @OperationLog(module = "工序管理", businessType = OperationTypeEnum.UPDATE, operation = "开始工序")
    @PostMapping("/{recordId}/start")
    public Result<Void> startProcess(@PathVariable Long recordId, @Valid @RequestBody StartProcessDTO dto) {
        processService.startProcess(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "完成工序")
    @OperationLog(module = "工序管理", businessType = OperationTypeEnum.UPDATE, operation = "完成工序")
    @PostMapping("/{recordId}/finish")
    public Result<Void> finishProcess(@PathVariable Long recordId, @RequestParam String processType) {
        processService.finishProcess(recordId, processType);
        return Result.success();
    }
}
