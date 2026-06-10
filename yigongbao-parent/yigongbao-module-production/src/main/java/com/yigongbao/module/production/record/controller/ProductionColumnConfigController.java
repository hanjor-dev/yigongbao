package com.yigongbao.module.production.record.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.record.dto.SaveProductionColumnConfigDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.ProductionColumnConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 生产流转卡列配置 Controller
 *
 * @author hanjor
 * @date 2026-06-10
 */
@Tag(name = "生产列配置", description = "生产流转卡列表列显示配置")
@RestController
@RequestMapping("/production/column-config")
@RequiredArgsConstructor
public class ProductionColumnConfigController {

    private final IProductionRecordService productionRecordService;

    @Operation(summary = "获取列配置")
    @GetMapping
    public Result<ProductionColumnConfigVO> getColumnConfig() {
        return Result.success(productionRecordService.getColumnConfig());
    }

    @Operation(summary = "保存列配置")
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.UPDATE, operation = "保存列配置")
    @PostMapping
    public Result<Void> saveColumnConfig(@Validated @RequestBody SaveProductionColumnConfigDTO dto) {
        productionRecordService.saveColumnConfig(dto);
        return Result.success();
    }
}
