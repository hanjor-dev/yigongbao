package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计文件管理控制器
 * 负责数据包、可视化模型、设计报告的上传和删除
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "设计文件管理")
@RestController
@RequestMapping("/api/design/workorder/{orderId}")
@RequiredArgsConstructor
public class DesignFileController {

    private final DesignFileService designFileService;

    // ==================== 数据包 ====================

    @Operation(summary = "上传打印文件数据包")
    @PostMapping("/package/upload")
    public Result<DesignPackageVO> uploadPackage(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "压缩包文件") @RequestParam("file") MultipartFile file) {
        DesignPackageVO result = designFileService.uploadPackage(orderId, file);
        return Result.success(result);
    }

    @Operation(summary = "删除数据包")
    @DeleteMapping("/package/{packageId}")
    public Result<Void> deletePackage(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        designFileService.deletePackage(orderId, packageId);
        return Result.success();
    }

    @Operation(summary = "获取数据包列表")
    @GetMapping("/packages")
    public Result<List<DesignPackageVO>> listPackages(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        List<DesignPackageVO> result = designFileService.listPackages(orderId);
        return Result.success(result);
    }

    // ==================== 可视化模型 ====================

    @Operation(summary = "上传可视化模型")
    @PostMapping("/visual-model/upload")
    public Result<DesignModelVO> uploadModel(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "模型文件") @RequestParam("file") MultipartFile file) {
        DesignModelVO result = designFileService.uploadModel(orderId, file);
        return Result.success(result);
    }

    @Operation(summary = "删除可视化模型")
    @DeleteMapping("/visual-model/{modelId}")
    public Result<Void> deleteModel(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "模型ID") @PathVariable Long modelId) {
        designFileService.deleteModel(orderId, modelId);
        return Result.success();
    }

    @Operation(summary = "获取可视化模型列表")
    @GetMapping("/visual-models")
    public Result<List<DesignModelVO>> listModels(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        List<DesignModelVO> result = designFileService.listModels(orderId);
        return Result.success(result);
    }

    // ==================== 设计报告 ====================

    @Operation(summary = "上传设计报告")
    @PostMapping("/report/upload")
    public Result<FileVO> uploadReport(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "报告文件") @RequestParam("file") MultipartFile file) {
        FileVO result = designFileService.uploadReport(orderId, file);
        return Result.success(result);
    }

    @Operation(summary = "删除设计报告")
    @DeleteMapping("/report/{fileId}")
    public Result<Void> deleteReport(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        designFileService.deleteReport(orderId, fileId);
        return Result.success();
    }

    @Operation(summary = "获取设计报告")
    @GetMapping("/report")
    public Result<FileVO> getReport(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        FileVO result = designFileService.getReport(orderId);
        return Result.success(result);
    }
}
