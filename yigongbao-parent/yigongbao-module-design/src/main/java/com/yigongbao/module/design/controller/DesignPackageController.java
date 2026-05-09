package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignPackageFileVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 打印文件数据包管理 Controller
 * <p>
 * 负责数据包的上传（含 ZIP 解析）、删除和列表查询。
 * 打印信息填写见 {@link DesignPrintInfoController}。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "数据包管理", description = "打印文件数据包上传（ZIP 解析）、删除、列表查询")
@RestController
@RequestMapping("/design")
@RequiredArgsConstructor
@RequireSign
public class DesignPackageController {

    private final DesignFileService designFileService;

    /**
     * 上传打印文件数据包（ZIP/RAR/7Z/TAR，后端自动解析有效文件）
     */
    @Operation(summary = "上传打印文件数据包")
    @PostMapping("/package/upload")
    public Result<DesignPackageVO> uploadPackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "压缩包文件") @RequestParam("file") MultipartFile file) {
        return Result.success(designFileService.uploadPackage(orderId, file));
    }

    /**
     * 删除数据包（若已有打印信息则拒绝）
     */
    @Operation(summary = "删除数据包")
    @DeleteMapping("/package/{packageId}")
    public Result<Void> deletePackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        designFileService.deletePackage(orderId, packageId);
        return Result.success();
    }

    /**
     * 获取订单下所有数据包列表（含包内文件）
     */
    @Operation(summary = "获取数据包列表")
    @GetMapping("/packages")
    public Result<List<DesignPackageVO>> listPackages(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        return Result.success(designFileService.listPackages(orderId));
    }

    /**
     * 获取数据包包内文件列表
     */
    @Operation(summary = "获取数据包包内文件列表")
    @GetMapping("/package/{packageId}/files")
    public Result<List<DesignPackageFileVO>> listPackageFiles(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        return Result.success(designFileService.listPackageFiles(orderId, packageId));
    }
}
