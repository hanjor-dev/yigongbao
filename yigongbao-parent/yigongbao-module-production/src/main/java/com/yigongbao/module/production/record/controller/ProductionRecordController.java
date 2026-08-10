package com.yigongbao.module.production.record.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.CancelPreviewVO;
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.ProcessingCenterPrintersVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.basic.file.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Operation(summary = "查询流转卡详情")
    @GetMapping("/{id}")
    public Result<ProductionRecordVO> getRecordDetail(@PathVariable Long id) {
        return Result.success(recordService.getRecordDetail(id));
    }

    @Operation(summary = "下载设计数据包")
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.DOWNLOAD, operation = "下载设计数据包")
    @PostMapping("/{id}/download-package")
    public Result<String> downloadDataPackage(@PathVariable Long id) {
        return Result.success(recordService.downloadDataPackage(id));
    }

    @Operation(summary = "自动生成生产批号（预览，不写库）")
    @GetMapping("/{id}/generate-batch-no")
    public Result<String> generateBatchNo(@PathVariable Long id) {
        return Result.success(recordService.generateBatchNo(id));
    }

    @Operation(summary = "提交生产批号")
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.SUBMIT, operation = "提交生产批号")
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
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.ASSIGN, operation = "分配打印设备")
    @PostMapping("/{id}/assign-device")
    public Result<Void> assignDevice(@PathVariable Long id, @Valid @RequestBody AssignDeviceDTO dto) {
        recordService.assignDevice(id, dto);
        return Result.success();
    }

    @Operation(summary = "强制释放打印设备配置")
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.CANCEL, operation = "强制释放打印设备配置")
    @PostMapping("/{id}/release-device")
    public Result<Void> releaseDevice(@PathVariable Long id) {
        recordService.releaseDevice(id);
        return Result.success();
    }

    @Operation(summary = "流转卡取消预查询")
    @GetMapping("/{id}/cancel-preview")
    public Result<CancelPreviewVO> getCancelPreview(@PathVariable Long id) {
        return Result.success(recordService.getCancelPreview(id));
    }

    @Operation(summary = "生成流转卡Excel")
    @GetMapping("/{id}/excel")
    public Result<FileVO> generateFlowCardExcel(@PathVariable Long id) {
        FileVO fileVO = recordService.getOrGenerateFlowCardExcel(id);
        return Result.success(fileVO);
    }

    @Operation(summary = "导出生产产品台账Excel")
    @OperationLog(module = "生产管理", businessType = OperationTypeEnum.EXPORT, operation = "导出生产产品台账")
    @PostMapping("/product-ledger/export")
    public void exportProductLedger(@Valid @RequestBody ProductLedgerExportDTO dto, HttpServletResponse response) throws IOException {
        byte[] excelBytes = recordService.exportProductLedger(dto);

        // 注：生产产品台账是批量导出，不添加患者姓名
        String fileName = "生产产品台账_" + java.time.LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
}
