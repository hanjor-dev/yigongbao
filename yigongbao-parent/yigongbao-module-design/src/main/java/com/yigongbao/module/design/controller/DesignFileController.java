package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.LinkFilesDTO;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计文件管理控制器
 * 数据包：直接上传（需解析压缩包）
 * 模型/报告：关联已上传的文件
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "设计文件管理")
@RestController
@RequestMapping("/design")
@RequiredArgsConstructor
public class DesignFileController {

    private final DesignFileService designFileService;

    // ==================== 数据包 ====================

    @Operation(summary = "上传打印文件数据包")
    @PostMapping("/package/upload")
    public Result<DesignPackageVO> uploadPackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "压缩包文件") @RequestParam("file") MultipartFile file) {
        DesignPackageVO result = designFileService.uploadPackage(orderId, file);
        return Result.success(result);
    }

    @Operation(summary = "删除数据包")
    @DeleteMapping("/package/{packageId}")
    public Result<Void> deletePackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        designFileService.deletePackage(orderId, packageId);
        return Result.success();
    }

    @Operation(summary = "获取数据包列表")
    @GetMapping("/packages")
    public Result<List<DesignPackageVO>> listPackages(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        List<DesignPackageVO> result = designFileService.listPackages(orderId);
        return Result.success(result);
    }

    // ==================== 可视化模型 ====================

    @Operation(summary = "批量关联可视化模型")
    @PostMapping("/models/link")
    public Result<List<DesignModelVO>> linkModels(@Valid @RequestBody LinkFilesDTO dto) {
        List<DesignModelVO> result = designFileService.linkModels(dto.getOrderId(), dto.getFileIds());
        return Result.success(result);
    }

    @Operation(summary = "删除可视化模型")
    @DeleteMapping("/model/{modelId}")
    public Result<Void> deleteModel(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "模型ID") @PathVariable Long modelId) {
        designFileService.deleteModel(orderId, modelId);
        return Result.success();
    }

    @Operation(summary = "获取可视化模型列表")
    @GetMapping("/models")
    public Result<List<DesignModelVO>> listModels(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        List<DesignModelVO> result = designFileService.listModels(orderId);
        return Result.success(result);
    }

    // ==================== 设计报告 ====================

    @Operation(summary = "关联设计报告")
    @PostMapping("/report/link")
    public Result<FileVO> linkReport(@Valid @RequestBody LinkFilesDTO dto) {
        // 设计报告只取第一个文件
        String fileId = dto.getFileIds().get(0);
        FileVO result = designFileService.linkReport(dto.getOrderId(), fileId);
        return Result.success(result);
    }

    @Operation(summary = "删除设计报告")
    @DeleteMapping("/report/{fileId}")
    public Result<Void> deleteReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        designFileService.deleteReport(orderId, fileId);
        return Result.success();
    }

    @Operation(summary = "获取设计报告")
    @GetMapping("/report")
    public Result<FileVO> getReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        FileVO result = designFileService.getReport(orderId);
        return Result.success(result);
    }
}
