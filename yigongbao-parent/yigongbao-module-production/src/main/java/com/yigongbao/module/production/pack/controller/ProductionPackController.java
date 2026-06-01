package com.yigongbao.module.production.pack.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.pack.service.IProductionPackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 包装管理
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "包装管理")
@RestController
@RequestMapping("/production/pack")
@RequiredArgsConstructor
public class ProductionPackController {

    private final IProductionPackService packService;

    @Operation(summary = "填写包装信息")
    @OperationLog(module = "包装管理", businessType = OperationTypeEnum.UPDATE, operation = "填写包装信息")
    @PutMapping("/{recordId}/fill")
    public Result<Void> fillPackInfo(@PathVariable Long recordId, @Valid @RequestBody FillPackDTO dto) {
        packService.fillPackInfo(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "包装完成，流转到入库")
    @OperationLog(module = "包装管理", businessType = OperationTypeEnum.TRANSFER, operation = "流转到入库")
    @PostMapping("/{recordId}/transfer")
    public Result<Void> transferToWarehouse(@PathVariable Long recordId) {
        packService.transferToWarehouse(recordId);
        return Result.success();
    }
}
