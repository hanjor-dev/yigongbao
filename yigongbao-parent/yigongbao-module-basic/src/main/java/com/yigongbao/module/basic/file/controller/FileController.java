package com.yigongbao.module.basic.file.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.basic.file.dto.FileListDTO;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件管理 Controller
 * 提供统一的文件上传、下载、删除、查询接口
 *
 * @author hanjor
 * @date 2026-03-25 11:00:00
 */
@Tag(name = "文件管理", description = "文件上传、下载、删除")
@RestController
@RequestMapping("/basic/file")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件（不关联业务）
     *
     * @param bizType 业务类型（字典 dict_code，如 10.1、10.4），通过 GET /system/select/biz-type-list 获取可选值
     */
    @Operation(summary = "上传文件（不关联业务）")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPLOAD,
            operation = "上传文件"
    )
    @PostMapping("/upload")
    public Result<FileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank(message = "业务类型不能为空") String bizType) {
        return Result.success(fileService.uploadFile(file, bizType));
    }

    /**
     * 上传并关联业务
     *
     * @param bizType 业务类型（字典 dict_code，如 10.1、10.4）
     * @param bizId 业务ID
     */
    @Operation(summary = "上传并关联业务")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPLOAD,
            operation = "上传文件并关联业务"
    )
    @PostMapping("/upload-and-link")
    public Result<FileVO> uploadAndLink(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank(message = "业务类型不能为空") String bizType,
            @RequestParam @NotNull(message = "业务ID不能为空") Long bizId) {
        return Result.success(fileService.uploadAndLink(file, bizType, bizId));
    }

    /**
     * 批量上传
     *
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId 业务ID
     */
    @Operation(summary = "批量上传")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPLOAD,
            operation = "批量上传文件"
    )
    @PostMapping("/upload-multiple")
    public Result<List<FileVO>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam @NotBlank(message = "业务类型不能为空") String bizType,
            @RequestParam @NotNull(message = "业务ID不能为空") Long bizId) {
        return Result.success(fileService.uploadMultiple(files, bizType, bizId));
    }

    /**
     * 查询文件列表
     *
     * @param dto 查询参数
     */
    @Operation(summary = "查询文件列表")
    @PostMapping("/list")
    public Result<List<FileVO>> listByBiz(@RequestBody FileListDTO dto) {
        return Result.success(fileService.listByBiz(dto.getBizType(), dto.getBizId()));
    }

    /**
     * 查询文件详情
     */
    @Operation(summary = "查询文件详情")
    @GetMapping("/{id}")
    public Result<FileVO> getById(@PathVariable String id) {
        return Result.success(fileService.getById(id));
    }

    /**
     * 下载文件
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    public void download(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        fileService.download(id, response);
    }

    /**
     * 删除文件
     */
    @Operation(summary = "删除文件")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除文件"
    )
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable String id) {
        fileService.deleteById(id);
        return Result.success();
    }
}
