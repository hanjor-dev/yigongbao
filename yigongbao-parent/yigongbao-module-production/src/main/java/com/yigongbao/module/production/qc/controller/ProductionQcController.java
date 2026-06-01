package com.yigongbao.module.production.qc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质检管理
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "质检管理")
@RestController
@RequestMapping("/production/qc")
@RequiredArgsConstructor
public class ProductionQcController {

    private final IProductionQcService qcService;

    @Operation(summary = "质检列表")
    @PostMapping("/list")
    public Result<IPage<ProductionRecordVO>> list(@RequestBody ProductionQcPageDTO dto) {
        return Result.success(qcService.listQcRecords(dto));
    }

    @Operation(summary = "获取待质检产品列表")
    @GetMapping("/{recordId}/products")
    public Result<List<ProductionProductVO>> getProducts(@PathVariable Long recordId) {
        return Result.success(qcService.listProductsByRecordId(recordId));
    }

    @Operation(summary = "标记产品质检合格")
    @OperationLog(module = "质检管理", businessType = OperationTypeEnum.UPDATE, operation = "标记产品质检合格")
    @PostMapping("/product/{productId}/pass")
    public Result<Void> markProductPass(@PathVariable Long productId) {
        qcService.markProductPass(productId);
        return Result.success();
    }

    @Operation(summary = "标记产品质检不合格")
    @OperationLog(module = "质检管理", businessType = OperationTypeEnum.UPDATE, operation = "标记产品质检不合格")
    @PostMapping("/product/{productId}/fail")
    public Result<Void> markProductFail(@PathVariable Long productId, @RequestParam String reason) {
        qcService.markProductFail(productId, reason);
        return Result.success();
    }

    @Operation(summary = "质检完成，流转到包装")
    @OperationLog(module = "质检管理", businessType = OperationTypeEnum.TRANSFER, operation = "流转到包装")
    @PostMapping("/{recordId}/transfer-to-pack")
    public Result<Void> transferToPacking(@PathVariable Long recordId) {
        qcService.transferToPacking(recordId);
        return Result.success();
    }
}
