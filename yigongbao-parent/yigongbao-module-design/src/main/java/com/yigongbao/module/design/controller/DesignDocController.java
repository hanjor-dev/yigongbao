package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 指令单/图纸生成与管理 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "指令单/图纸管理", description = "设计阶段指令单和图纸的生成、版本查询、下载、修订版上传")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignDocController {

    private final DesignDocService docService;

    /**
     * 同时生成指令单和图纸
     */
    @Operation(summary = "生成指令单和图纸")
    @PostMapping("/{orderId}/package/{packageId}/generate-docs")
    public Result<GenerateDocsResultVO> generateDocs(@PathVariable Long orderId,
                                                      @PathVariable Long packageId) {
        return Result.success(docService.generateDocs(orderId, packageId));
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
     * 下载指定版本的指令单（模板版）
     */
    @Operation(summary = "下载指令单（模板版）")
    @GetMapping("/{orderId}/package/{packageId}/instruction/download/{id}")
    public void downloadInstruction(@PathVariable Long orderId,
                                    @PathVariable Long packageId,
                                    @PathVariable Long id,
                                    HttpServletResponse response) throws IOException {
        docService.downloadInstruction(orderId, packageId, id, response);
    }

    /**
     * 下载指定版本的图纸（模板版）
     */
    @Operation(summary = "下载图纸（模板版）")
    @GetMapping("/{orderId}/package/{packageId}/drawing/download/{id}")
    public void downloadDrawing(@PathVariable Long orderId,
                                @PathVariable Long packageId,
                                @PathVariable Long id,
                                HttpServletResponse response) throws IOException {
        docService.downloadDrawing(orderId, packageId, id, response);
    }

    /**
     * 上传修订版指令单
     */
    @Operation(summary = "上传修订版指令单")
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
    @PostMapping("/{orderId}/package/{packageId}/drawing/upload-revised/{id}")
    public Result<Void> uploadRevisedDrawing(@PathVariable Long orderId,
                                              @PathVariable Long packageId,
                                              @PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        docService.uploadRevisedDrawing(orderId, packageId, id, file);
        return Result.success();
    }
}
