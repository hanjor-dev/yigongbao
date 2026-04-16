package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 指令单/图纸生成与管理 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "指令单/图纸管理", description = "设计阶段指令单和图纸的生成、版本查询、修订版上传（下载通过 GET /basic/file/download/{fileId}）")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignDocController {

    private final DesignDocService docService;

    /**
     * 生成指令单
     */
    @Operation(summary = "生成指令单")
    @PostMapping("/{orderId}/package/{packageId}/instruction/generate")
    public Result<DocItemVO> generateInstruction(@PathVariable Long orderId,
                                                  @PathVariable Long packageId) {
        return Result.success(docService.generateInstruction(orderId, packageId));
    }

    /**
     * 生成图纸
     */
    @Operation(summary = "生成图纸")
    @PostMapping("/{orderId}/package/{packageId}/drawing/generate")
    public Result<DocItemVO> generateDrawing(@PathVariable Long orderId,
                                              @PathVariable Long packageId) {
        return Result.success(docService.generateDrawing(orderId, packageId));
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
