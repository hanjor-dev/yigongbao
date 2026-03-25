package com.yigongbao.module.basic.file.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/basic/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件（不关联业务）
     */
    @PostMapping("/upload")
    public Result<FileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType) {
        return Result.success(fileService.uploadFile(file, bizType));
    }

    /**
     * 上传并关联业务
     */
    @PostMapping("/upload-and-link")
    public Result<FileVO> uploadAndLink(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType,
            @RequestParam Long bizId) {
        return Result.success(fileService.uploadAndLink(file, bizType, bizId));
    }

    /**
     * 批量上传
     */
    @PostMapping("/upload-multiple")
    public Result<List<FileVO>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam String bizType,
            @RequestParam Long bizId) {
        return Result.success(fileService.uploadMultiple(files, bizType, bizId));
    }

    /**
     * 查询文件列表
     */
    @GetMapping("/list/{bizType}/{bizId}")
    public Result<List<FileVO>> listByBiz(
            @PathVariable String bizType,
            @PathVariable Long bizId) {
        return Result.success(fileService.listByBiz(bizType, bizId));
    }

    /**
     * 查询文件详情
     */
    @GetMapping("/{id}")
    public Result<FileVO> getById(@PathVariable String id) {
        return Result.success(fileService.getById(id));
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public void download(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        fileService.download(id, response);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable String id) {
        fileService.deleteById(id);
        return Result.success();
    }
}
