package com.yigongbao.module.production.record.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.record.dto.CreateRecordDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 生产流转卡管理
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "生产流转卡管理")
@RestController
@RequestMapping("/production/record")
@RequiredArgsConstructor
public class ProductionRecordController {

    private final IProductionRecordService recordService;

    @Operation(summary = "分页查询生产列表")
    @PostMapping("/list")
    public Result<IPage<ProductionRecordVO>> list(@RequestBody ProductionRecordPageDTO dto) {
        return Result.success(recordService.pageRecords(dto));
    }

    @Operation(summary = "扫码查询流转卡")
    @GetMapping("/scan")
    public Result<ProductionRecordVO> scanRecord(@RequestParam String recordNo) {
        return Result.success(recordService.getByRecordNo(recordNo));
    }

    @Operation(summary = "创建生产流转卡")
    @PostMapping("/create")
    public Result<Long> createRecord(@Valid @RequestBody CreateRecordDTO dto) {
        return Result.success(recordService.createRecord(dto));
    }

    @Operation(summary = "查询流转卡详情")
    @GetMapping("/{id}")
    public Result<ProductionRecordVO> getRecordDetail(@PathVariable Long id) {
        return Result.success(recordService.getRecordDetail(id));
    }

    @Operation(summary = "下载设计数据包")
    @PostMapping("/{designPackageId}/download-package")
    public Result<Void> downloadDataPackage(@PathVariable Long designPackageId) {
        recordService.downloadDataPackage(designPackageId);
        return Result.success();
    }

    @Operation(summary = "获取流转卡二维码")
    @GetMapping("/{id}/qr-code")
    public Result<String> getQrCode(@PathVariable Long id) {
        return Result.success(recordService.getQrCodeUrl(id));
    }
}
