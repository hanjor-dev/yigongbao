package com.yigongbao.module.imaging.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.imaging.service.ImagingService;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 影像阅览控制器
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Tag(name = "影像阅览", description = "DCM影像文件与3D模型文件查询接口")
@SaCheckLogin
@RestController
@RequestMapping("/imaging")
@RequiredArgsConstructor
public class ImagingController {

    private final ImagingService imagingService;

    @Operation(summary = "获取订单的DCM影像数据包列表")
    @GetMapping("/dcm-packages")
    public Result<List<DcmPackageVO>> getDcmPackages(@RequestParam Long orderId) {
        return Result.success(imagingService.getDcmPackages(orderId));
    }

    @Operation(summary = "获取指定数据包内的模型文件列表（含颜色透明度）")
    @GetMapping("/package-model-files")
    public Result<List<PackageModelFileVO>> getPackageModelFiles(@RequestParam Long packageId) {
        return Result.success(imagingService.getPackageModelFiles(packageId));
    }

    @Operation(summary = "获取订单所有数据包内的模型文件（按包分组，含颜色透明度）")
    @GetMapping("/package-model-files/by-order")
    public Result<List<PackageModelGroupVO>> getPackageModelFilesByOrder(@RequestParam Long orderId) {
        return Result.success(imagingService.getPackageModelFilesByOrder(orderId));
    }

    @Operation(summary = "获取订单的可视化模型列表（含颜色透明度）")
    @GetMapping("/models")
    public Result<List<ModelVO>> getModels(@RequestParam Long orderId) {
        return Result.success(imagingService.getModels(orderId));
    }
}
