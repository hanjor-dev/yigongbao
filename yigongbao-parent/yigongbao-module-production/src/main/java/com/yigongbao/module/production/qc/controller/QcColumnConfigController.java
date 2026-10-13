package com.yigongbao.module.production.qc.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.qc.dto.SaveQcColumnConfigDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import com.yigongbao.module.production.qc.vo.QcColumnConfigVO;
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
 * 质检列表列配置 Controller
 */
@Tag(name = "质检列配置", description = "质检列表列显示配置")
@RestController
@RequestMapping("/production/qc/column-config")
@RequiredArgsConstructor
public class QcColumnConfigController {

    private final IProductionQcService productionQcService;

    @Operation(summary = "获取质检列配置")
    @GetMapping
    public Result<QcColumnConfigVO> getColumnConfig() {
        return Result.success(productionQcService.getColumnConfig());
    }

    @Operation(summary = "保存质检列配置")
    @OperationLog(module = "质检管理", businessType = OperationTypeEnum.UPDATE, operation = "保存质检列配置")
    @PostMapping
    public Result<Void> saveColumnConfig(@Validated @RequestBody SaveQcColumnConfigDTO dto) {
        productionQcService.saveColumnConfig(dto);
        return Result.success();
    }
}
