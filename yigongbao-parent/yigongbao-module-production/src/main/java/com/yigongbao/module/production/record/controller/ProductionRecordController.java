package com.yigongbao.module.production.record.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.ProcessingCenterPrintersVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "自动生成生产批号（预览，不写库）")
    @GetMapping("/{id}/generate-batch-no")
    public Result<String> generateBatchNo(@PathVariable Long id) {
        return Result.success(recordService.generateBatchNo(id));
    }

    @Operation(summary = "提交生产批号")
    @PostMapping("/{id}/submit-batch-no")
    public Result<Void> submitBatchNo(@PathVariable Long id, @Valid @RequestBody SubmitBatchNoDTO dto) {
        recordService.submitBatchNo(id, dto);
        return Result.success();
    }

    @Operation(summary = "设备配置详情")
    @GetMapping("/{id}/device-config")
    public Result<DeviceConfigVO> getDeviceConfig(@PathVariable Long id) {
        return Result.success(recordService.getDeviceConfig(id));
    }

    @Operation(summary = "获取打印机列表（按加工中心分组）")
    @GetMapping("/printers")
    public Result<List<ProcessingCenterPrintersVO>> listPrinters() {
        return Result.success(recordService.listPrinters());
    }

    @Operation(summary = "提交打印机配置")
    @PostMapping("/{id}/assign-device")
    public Result<Void> assignDevice(@PathVariable Long id, @Valid @RequestBody AssignDeviceDTO dto) {
        recordService.assignDevice(id, dto);
        return Result.success();
    }
}
