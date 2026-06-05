package com.yigongbao.module.design.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.LinkFilesDTO;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignModelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设计阶段附件管理 Controller
 * <p>
 * 负责可视化模型（3D 模型文件）和设计报告的关联、删除、查询。
 * 两类资源均通过"先调用通用文件上传接口获得 fileId，再关联到业务"的方式管理。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "设计附件管理", description = "可视化模型（关联/删除/查询）与设计报告（关联/删除/查询）")
@RestController
@RequestMapping("/design")
@RequiredArgsConstructor
public class DesignAttachmentController {

    private final DesignFileService designFileService;

    // ==================== 可视化模型 ====================

    /**
     * 批量关联可视化模型（fileIds 通过通用文件上传接口获得）
     */
    @Operation(summary = "批量关联可视化模型")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "关联可视化模型")
    @PostMapping("/models/link")
    public Result<List<DesignModelVO>> linkModels(@Valid @RequestBody LinkFilesDTO dto) {
        return Result.success(designFileService.linkModels(dto.getOrderId(), dto.getFileIds()));
    }

    /**
     * 删除可视化模型
     */
    @Operation(summary = "删除可视化模型")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.DELETE, operation = "删除可视化模型")
    @DeleteMapping("/model/{modelId}")
    public Result<Void> deleteModel(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "模型ID") @PathVariable Long modelId) {
        designFileService.deleteModel(orderId, modelId);
        return Result.success();
    }

    /**
     * 获取可视化模型列表
     */
    @Operation(summary = "获取可视化模型列表")
    @GetMapping("/models")
    public Result<List<DesignModelVO>> listModels(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        return Result.success(designFileService.listModels(orderId));
    }

    // ==================== 设计报告 ====================

    /**
     * 关联设计报告（每订单仅保留一份，重复关联自动覆盖）
     */
    @Operation(summary = "关联设计报告")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.UPLOAD, operation = "关联设计报告")
    @PostMapping("/report/link")
    public Result<FileVO> linkReport(@Valid @RequestBody LinkFilesDTO dto) {
        return Result.success(designFileService.linkReport(dto.getOrderId(), dto.getFileIds().get(0)));
    }

    /**
     * 删除设计报告
     */
    @Operation(summary = "删除设计报告")
    @OperationLog(module = "设计管理", businessType = OperationTypeEnum.DELETE, operation = "删除设计报告")
    @DeleteMapping("/report/{fileId}")
    public Result<Void> deleteReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        designFileService.deleteReport(orderId, fileId);
        return Result.success();
    }

    /**
     * 获取设计报告
     */
    @Operation(summary = "获取设计报告")
    @GetMapping("/report")
    public Result<FileVO> getReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        return Result.success(designFileService.getReport(orderId));
    }
}
