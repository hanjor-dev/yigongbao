package com.yigongbao.module.design.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignQrImageService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DesignQrImageVO;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.design.vo.ScreenshotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 指令单/图纸管理 Controller
 * <p>
 * 生成逻辑已内化为"按需自动生成"：
 * - 线下模式：调用 download 接口，后端检测数据变化后按需生成并流式返回文件
 * - 在线模式：调用 preview-url 接口，后端按需生成后返回可访问的 URL
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "指令单/图纸管理", description = "设计阶段指令单和图纸的下载/预览（按需自动生成）、版本查询、修订版上传")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignDocController {

    private final DesignDocService docService;
    private final DesignQrImageService qrImageService;
    private final DesignScreenshotService screenshotService;

    /**
     * 查询订单当前图纸二维码图片。
     */
    @Operation(summary = "查询订单当前图纸二维码图片")
    @GetMapping("/{orderId}/qr-image")
    public Result<DesignQrImageVO> getQrImage(@PathVariable Long orderId) {
        return Result.success(qrImageService.getCurrent(orderId));
    }

    /**
     * 上传/替换订单当前图纸二维码图片。
     */
    @Operation(summary = "上传订单图纸二维码图片")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "上传图纸二维码图片")
    @PostMapping("/{orderId}/qr-image")
    public Result<DesignQrImageVO> uploadQrImage(@PathVariable Long orderId,
                                                  @RequestParam("file") MultipartFile file) {
        return Result.success(qrImageService.upload(orderId, file));
    }

    /**
     * 下载指令单（线下模式）
     * 按需自动生成：若打印信息发生变化则重新生成，否则复用已有文件
     */
    @Operation(summary = "下载指令单（线下模式，按需自动生成）")
    @GetMapping("/{orderId}/package/{packageId}/instruction/download")
    public void downloadInstruction(@PathVariable Long orderId,
                                    @PathVariable Long packageId,
                                    HttpServletResponse response) {
        docService.downloadInstruction(orderId, packageId, response);
    }

    /**
     * 下载图纸（线下模式）
     * 按需自动生成：若打印信息发生变化则重新生成，否则复用已有文件
     */
    @Operation(summary = "下载图纸（线下模式，按需自动生成）")
    @GetMapping("/{orderId}/package/{packageId}/drawing/download")
    public void downloadDrawing(@PathVariable Long orderId,
                                @PathVariable Long packageId,
                                HttpServletResponse response) {
        docService.downloadDrawing(orderId, packageId, response);
    }

    /**
     * 获取指令单预览 URL（在线模式）
     * 按需自动生成：若打印信息发生变化则重新生成，否则复用已有文件
     */
    @Operation(summary = "获取指令单预览 URL（在线模式，按需自动生成）")
    @GetMapping("/{orderId}/package/{packageId}/instruction/preview-url")
    public Result<DocItemVO> getInstructionPreviewUrl(@PathVariable Long orderId,
                                                       @PathVariable Long packageId) {
        return Result.success(docService.getInstructionPreviewUrl(orderId, packageId));
    }

    /**
     * 获取图纸预览 URL（在线模式）
     * 按需自动生成：若打印信息发生变化则重新生成，否则复用已有文件
     */
    @Operation(summary = "获取图纸预览 URL（在线模式，按需自动生成）")
    @GetMapping("/{orderId}/package/{packageId}/drawing/preview-url")
    public Result<DocItemVO> getDrawingPreviewUrl(@PathVariable Long orderId,
                                                   @PathVariable Long packageId) {
        return Result.success(docService.getDrawingPreviewUrl(orderId, packageId));
    }

    /**
     * 查询指令单历史版本列表
     */
    @Operation(summary = "查询指令单版本列表")
    @GetMapping("/{orderId}/package/{packageId}/instruction/versions")
    public Result<List<DesignDocVersionVO>> listInstructionVersions(@PathVariable Long orderId,
                                                                     @PathVariable Long packageId) {
        return Result.success(docService.listInstructionVersions(orderId, packageId));
    }

    /**
     * 查询图纸历史版本列表
     */
    @Operation(summary = "查询图纸版本列表")
    @GetMapping("/{orderId}/package/{packageId}/drawing/versions")
    public Result<List<DesignDocVersionVO>> listDrawingVersions(@PathVariable Long orderId,
                                                                 @PathVariable Long packageId) {
        return Result.success(docService.listDrawingVersions(orderId, packageId));
    }

    /**
     * 上传修订版指令单
     */
    @Operation(summary = "上传修订版指令单")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "上传修订版指令单")
    @PostMapping("/{orderId}/package/{packageId}/instruction/upload-revised/{id}")
    public Result<Void> uploadRevisedInstruction(@PathVariable Long orderId,
                                                  @PathVariable Long packageId,
                                                  @PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        docService.uploadRevisedInstruction(orderId, packageId, id, file);
        return Result.success();
    }

    /**
     * 上传修订版图纸
     */
    @Operation(summary = "上传修订版图纸")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "上传修订版图纸")
    @PostMapping("/{orderId}/package/{packageId}/drawing/upload-revised/{id}")
    public Result<Void> uploadRevisedDrawing(@PathVariable Long orderId,
                                              @PathVariable Long packageId,
                                              @PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        docService.uploadRevisedDrawing(orderId, packageId, id, file);
        return Result.success();
    }

    /**
     * 确认图纸（在线模式）
     * 设计师预览生成结果满意后调用，is_confirmed 置为 1。
     * 若之后重新生成图纸，确认状态自动重置，需再次确认。
     */
    @Operation(summary = "确认图纸（在线模式）")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPDATE, operation = "确认图纸")
    @PostMapping("/{orderId}/package/{packageId}/drawing/confirm/{id}")
    public Result<Void> confirmDrawing(@PathVariable Long orderId,
                                       @PathVariable Long packageId,
                                       @PathVariable Long id) {
        docService.confirmDrawing(orderId, packageId, id);
        return Result.success();
    }

    /**
     * 确认指令单（在线模式）
     * 设计师确认指令单内容无误后调用，is_confirmed 置为 1。
     * 若之后重新生成指令单，确认状态自动重置，需再次确认。
     */
    @Operation(summary = "确认指令单（在线模式）")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPDATE, operation = "确认指令单")
    @PostMapping("/{orderId}/package/{packageId}/instruction/confirm/{id}")
    public Result<Void> confirmInstruction(@PathVariable Long orderId,
                                            @PathVariable Long packageId,
                                            @PathVariable Long id) {
        docService.confirmInstruction(orderId, packageId, id);
        return Result.success();
    }

    /**
     * 上传数据包文件截图（upsert：有则覆盖，无则新增）
     */
    @Operation(summary = "上传数据包文件截图")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "上传数据包文件截图")
    @PostMapping("/{orderId}/package/{packageId}/files/{packageFileId}/screenshot")
    public Result<ScreenshotVO> saveScreenshot(@PathVariable Long orderId,
                                               @PathVariable Long packageId,
                                               @PathVariable Long packageFileId,
                                               @RequestParam("file") MultipartFile file) {
        return Result.success(screenshotService.saveScreenshot(packageId, packageFileId, file));
    }

    /**
     * 查询数据包文件截图
     */
    @Operation(summary = "查询数据包文件截图")
    @GetMapping("/{orderId}/package/{packageId}/files/{packageFileId}/screenshot")
    public Result<ScreenshotVO> getScreenshot(@PathVariable Long orderId,
                                              @PathVariable Long packageId,
                                              @PathVariable Long packageFileId) {
        return Result.success(screenshotService.getScreenshot(packageId, packageFileId));
    }
}
