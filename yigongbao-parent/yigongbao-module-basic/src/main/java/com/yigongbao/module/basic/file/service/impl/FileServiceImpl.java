package com.yigongbao.module.basic.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.config.FileStorageProperties;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.Downloader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.core.collection.CollUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件存储服务实现类
 * 基于 x-file-storage 框架，提供统一的文件存储能力
 * <p>
 * 上传：框架自动调用 FileRecorder 保存记录
 * 下载：通过框架下载后写入响应流
 * 删除：先删存储平台文件，再删数据库记录
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileStorageService fileStorageService;
    private final FileRecorderService fileRecorderService;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public FileVO uploadFile(MultipartFile file, String bizType) {
        return doUpload(file, bizType, null);
    }

    @Override
    public FileVO uploadAndLink(MultipartFile file, String bizType, Long bizId) {
        return doUpload(file, bizType, bizId);
    }

    @Override
    public List<FileVO> uploadMultiple(MultipartFile[] files, String bizType) {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        List<FileVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                results.add(uploadFile(file, bizType));
            }
        }
        return results;
    }

    @Override
    public List<FileVO> uploadMultipleWithBizId(MultipartFile[] files, String bizType, Long bizId) {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        List<FileVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                results.add(uploadAndLink(file, bizType, bizId));
            }
        }
        return results;
    }

    @Override
    public FileVO linkFile(String fileId, String bizType, Long bizId) {
        log.info("关联文件到业务，fileId={}, bizType={}, bizId={}", fileId, bizType, bizId);
        try {
            // 1. 校验文件是否存在
            FileDetail detail = fileRecorderService.getDetailById(fileId);
            if (detail == null) {
                log.warn("文件不存在，fileId={}", fileId);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }

            // 2. 校验 bizType 是否为合法的字典编码
            FileBizTypeEnum fileBizTypeEnum = FileBizTypeEnum.getByDictCode(bizType);
            if (fileBizTypeEnum == null) {
                log.warn("业务类型不合法，bizType={}", bizType);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "bizType");
            }

            // 3. 更新文件的业务关联信息
            detail.setObjectType(bizType);
            detail.setObjectId(bizId != null ? bizId.toString() : null);
            fileRecorderService.updateById(detail);

            log.info("文件关联成功，fileId={}, bizType={}, bizId={}", fileId, bizType, bizId);
            return fileRecorderService.toFileVO(detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("关联文件异常，fileId={}", fileId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public FileVO getById(String id) {
        log.info("根据ID查询文件信息，id={}", id);
        try {
            FileDetail detail = fileRecorderService.getDetailById(id);
            if (detail == null) {
                log.warn("文件不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            FileVO vo = fileRecorderService.toFileVO(detail);
            log.info("查询文件信息成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询文件信息异常，id={}", id, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public List<FileVO> listByIds(List<String> ids) {
        log.info("批量查询文件信息，ids={}", ids);
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<FileDetail> details = fileRecorderService.listByIds(ids);
        return details.stream().map(fileRecorderService::toFileVO).toList();
    }

    @Override
    public List<FileVO> listByBiz(String bizType, Long bizId) {
        log.info("查询文件列表，bizType={}, bizId={}", bizType, bizId);
        try {
            LambdaQueryWrapper<FileDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileDetail::getObjectType, bizType)
                    .eq(bizId != null, FileDetail::getObjectId,
                            bizId != null ? bizId.toString() : null)
                    .orderByDesc(FileDetail::getCreateTime);
            List<FileDetail> details = fileRecorderService.list(wrapper);
            List<FileVO> vos = details.stream()
                    .map(fileRecorderService::toFileVO)
                    .toList();
            log.info("查询文件列表成功，共{}条", vos.size());
            return vos;
        } catch (Exception e) {
            log.error("查询文件列表异常，bizType={}", bizType, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public void download(String id, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        log.info("下载文件，id={}", id);
        try {
            FileDetail detail = fileRecorderService.getDetailById(id);
            if (detail == null) {
                log.warn("文件不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            FileInfo fileInfo = fileRecorderService.getById(id);
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(
                            detail.getOriginalFilename(), StandardCharsets.UTF_8));
            // 使用框架的 outputStream 方法直接将文件写入响应流
            Downloader downloader = fileStorageService.download(fileInfo);
            downloader.setProgressMonitor((progressSize, allSize) ->
                    log.debug("文件下载进度，id={}, progress={}/{}",
                            id, progressSize, allSize));
            downloader.outputStream(response.getOutputStream());
            log.info("下载文件成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载文件异常，id={}", id, e);
            // 区分文件不存在异常
            String msg = e.getMessage();
            if (msg != null && (msg.contains("not exist") || msg.contains("不存在")
                    || msg.contains("No such") || msg.contains("404"))) {
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public void deleteById(String id) {
        log.info("删除文件，id={}", id);
        try {
            FileDetail detail = fileRecorderService.getDetailById(id);
            if (detail == null) {
                log.warn("文件不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            // 先删数据库记录（可回滚），再删存储平台文件
            fileRecorderService.removeById(id);
            fileStorageService.delete(detail.getUrl());
            log.info("删除文件成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除文件异常，id={}", id, e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_DELETE_FAILED);
        }
    }

    /**
     * 上传核心逻辑
     *
     * @param bizType 业务类型（字典 dict_code，如：10.1）
     */
    private FileVO doUpload(MultipartFile file, String bizType, Long bizId) {
        log.info("上传文件，bizType={}, bizId={}, fileName={}",
                bizType, bizId, file.getOriginalFilename());
        try {
            validateFile(file);
            // 校验 bizType 是否为合法的字典编码
            FileBizTypeEnum fileBizTypeEnum = FileBizTypeEnum.getByDictCode(bizType);
            if (fileBizTypeEnum == null) {
                log.warn("业务类型不合法，bizType={}", bizType);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "bizType");
            }
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            // 存储路径使用枚举 code（如 image_data）
            String storagePath = fileBizTypeEnum.getCode() + "/" + datePath + "/";
            // 数据库 object_type 字段存储 dict_code（如 10.1）
            String objectType = fileBizTypeEnum.getDictCode();
            // 框架上传，上传成功后自动调用 FileRecorderService.save(FileInfo)
            FileInfo fileInfo = fileStorageService.of(file)
                    .setPath(storagePath)
                    .setObjectType(objectType)
                    .setObjectId(bizId != null ? bizId.toString() : null)
                    .upload();
            log.info("上传文件成功，id={}, url={}", fileInfo.getId(), fileInfo.getUrl());
            // 从数据库查询完整记录（包含自动填充的 createTime 等字段）
            FileDetail detail = fileRecorderService.getDetailById(fileInfo.getId());
            return fileRecorderService.toFileVO(detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件异常，bizType={}", bizType, e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    /**
     * 校验文件大小、扩展名和文件名安全性
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            log.warn("文件大小超出限制，size={}, maxSize={}",
                    file.getSize(), fileStorageProperties.getMaxFileSize());
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_SIZE_EXCEEDED);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
        }
        // 校验文件名安全性，过滤路径遍历等非法字符
        if (!isValidFilename(originalFilename)) {
            log.warn("文件名包含非法字符，fileName={}", originalFilename);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_FILENAME_ILLEGAL);
        }
        String ext = getFileExt(originalFilename).toLowerCase();
        boolean allowed = Arrays.stream(fileStorageProperties.getAllowedExtensions())
                .anyMatch(e -> e.equals(ext));
        if (!allowed) {
            log.warn("不支持的文件类型，ext={}", ext);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
        }
    }

    private String getFileExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 校验文件名是否安全，过滤路径遍历等危险字符
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        // 禁止路径遍历字符
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        // 禁止 Windows 非法字符
        if (filename.contains("<") || filename.contains(">") || filename.contains(":")
                || filename.contains("\"") || filename.contains("|") || filename.contains("?")
                || filename.contains("*")) {
            return false;
        }
        // 禁止控制字符
        for (char c : filename.toCharArray()) {
            if (c < 0x20 || c == 0x7F) {
                return false;
            }
        }
        return true;
    }
}
