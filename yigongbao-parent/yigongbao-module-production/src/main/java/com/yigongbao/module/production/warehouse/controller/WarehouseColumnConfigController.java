package com.yigongbao.module.production.warehouse.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.warehouse.dto.SaveWarehouseColumnConfigDTO;
import com.yigongbao.module.production.warehouse.service.IWarehouseService;
import com.yigongbao.module.production.warehouse.vo.WarehouseColumnConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓储列表列配置 Controller
 */
@Tag(name = "仓储列配置", description = "仓储列表列显示配置")
@RestController
@RequestMapping("/production/warehouse/column-config")
@RequiredArgsConstructor
public class WarehouseColumnConfigController {

    private final IWarehouseService warehouseService;

    @Operation(summary = "获取仓储列配置")
    @GetMapping
    public Result<WarehouseColumnConfigVO> getColumnConfig() {
        return Result.success(warehouseService.getColumnConfig());
    }

    @Operation(summary = "保存仓储列配置")
    @OperationLog(module = "仓储管理", businessType = OperationTypeEnum.UPDATE, operation = "保存仓储列配置")
    @PostMapping
    public Result<Void> saveColumnConfig(@Validated @RequestBody SaveWarehouseColumnConfigDTO dto) {
        warehouseService.saveColumnConfig(dto);
        return Result.success();
    }
}
