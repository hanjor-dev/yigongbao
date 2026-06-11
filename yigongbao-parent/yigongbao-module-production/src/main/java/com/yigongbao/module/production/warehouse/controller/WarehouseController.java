package com.yigongbao.module.production.warehouse.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.service.IWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 仓储管理 Controller
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Tag(name = "仓储管理")
@RestController
@RequestMapping("/production/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final IWarehouseService warehouseService;

    @Operation(summary = "产品入库")
    @OperationLog(module = "仓储管理", businessType = OperationTypeEnum.UPDATE, operation = "产品入库")
    @PostMapping("/in/products/{productId}")
    public Result<Void> warehouseInProduct(
        @PathVariable Long productId,
        @Validated @RequestBody WarehouseInProductDTO dto) {
        warehouseService.warehouseInProduct(productId, dto);
        return Result.success();
    }

    @Operation(summary = "产品出库")
    @OperationLog(module = "仓储管理", businessType = OperationTypeEnum.UPDATE, operation = "产品出库")
    @PostMapping("/out/products/{productId}")
    public Result<Void> warehouseOutProduct(
        @PathVariable Long productId,
        @Validated @RequestBody WarehouseOutProductDTO dto) {
        warehouseService.warehouseOutProduct(productId, dto);
        return Result.success();
    }
}
