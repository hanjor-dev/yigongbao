package com.yigongbao.module.basic.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.config.FileStorageProperties;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.provider.FileUploadConfigProvider;
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
import cn.hutool.core.util.StrUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储服务实现类
 *
 * <p>基于 x-file-storage 框架，提供统一的文件存储能力。</p>
 *
 * <h3>核心流程</h3>
 * <pre>
 * 上传：validateFile（全局安全校验）
 *       → bizType 合法性校验
 *       → FileUploadConfigProvider（按业务类型读 sys_config，校验后缀和大小）
 *       → x-file-storage 写入存储平台
 *       → FileRecorder 自动将 FileInfo 持久化到 file_detail 表
 * 下载：通过框架 Downloader 直接写入响应流
 * 删除：先删 file_detail 记录（可回滚），再删存储平台文件
 * </pre>
 *
 * <h3>校验分层说明</h3>
 * <ul>
 *   <li>{@link #validateFile} — 全局兜底：文件非空、大小不超全局上限（2GB）、文件名安全性</li>
 *   <li>{@link FileUploadConfigProvider} — 业务类型级：后缀白名单和大小上限，配置来自 sys_config</li>
 *   <li>业务提交层（Order/Design）— 只校验必填性和文件 ID 存在性，不重复校验后缀和大小</li>
 * </ul>
 *
 * <h3>模块边界</h3>
 * <p>module-basic 不依赖 module-system。{@link FileUploadConfigProvider} 接口定义在 module-basic，
 * module-system 提供 {@code @Primary} 实现，通过 ConfigService 读取 sys_config 动态返回配置。
 * 单元测试或独立部署时，{@code NoOpFileUploadConfigProvider}（无限制）自动生效。</p>
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
    private final FileUploadConfigProvider fileUploadConfigProvider;

    // ==================== 上传 ====================

    @Override
    public FileVO uploadFile(MultipartFile file, String bizType) {
        return doUpload(file, bizType, null);
    }

    @Override
    public FileVO uploadBytes(byte[] bytes, String filename, String bizType) {
        return doUpload(new ByteArrayMultipartFile(bytes, filename), bizType, null);
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

    // ==================== 关联 ====================

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

    // ==================== 查询 ====================

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

    // ==================== 下载 ====================

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
            // 设置响应头，触发浏览器文件下载
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(
                            detail.getOriginalFilename(), StandardCharsets.UTF_8));
            // 使用框架的 outputStream 方法直接将文件写入响应流，避免内存中转
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
            // 区分文件在存储平台不存在的情况
            String msg = e.getMessage();
            if (msg != null && (msg.contains("not exist") || msg.contains("不存在")
                    || msg.contains("No such") || msg.contains("404"))) {
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    // ==================== 删除 ====================

    @Override
    public void deleteById(String id) {
        log.info("删除文件，id={}", id);
        try {
            FileDetail detail = fileRecorderService.getDetailById(id);
            if (detail == null) {
                log.warn("文件不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
            // 先删数据库记录（事务可回滚），再删存储平台文件（不可回滚）
            // 若存储平台删除失败，数据库记录已删，文件成为孤儿文件，需运维清理
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

    // ==================== 校验工具方法 ====================

    /**
     * 解析允许上传的文件扩展名配置字符串为集合
     * <p>对配置值执行 split(",") → trim → toLowerCase → 过滤空串</p>
     *
     * @param config   配置值（逗号分隔，如 ".zip,.rar,.7z"）；为 null/blank 时使用 fallback
     * @param fallback 兜底值（同格式）；config 和 fallback 均为 null/blank 时返回空集合
     * @return 小写扩展名集合，含点（如 {".zip", ".rar", ".7z"}）
     */
    @Override
    public Set<String> parseAllowedExtensions(String config, String fallback) {
        String source = StrUtil.isNotBlank(config) ? config : fallback;
        if (StrUtil.isBlank(source)) {
            return Set.of();
        }
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 校验文件列表中每个文件的扩展名是否在允许集合内
     * <p>遍历所有文件，任意一个不符合即抛出异常，并在日志中记录具体文件 ID 和扩展名。</p>
     *
     * @param files        文件列表；为空时直接返回，不校验
     * @param allowedExts  允许的扩展名集合（含点，如 {".pdf", ".docx"}）
     * @param categoryName 文件类别名称，用于日志和异常提示（如 "影像数据包"）
     * @throws BusinessException 任一文件扩展名不合法时抛出 ATTACHMENT_TYPE_NOT_ALLOWED
     */
    @Override
    public void assertAllFileTypesAllowed(List<FileVO> files, Set<String> allowedExts, String categoryName) {
        if (CollUtil.isEmpty(files)) {
            return;
        }
        for (FileVO file : files) {
            String name = file.getFileName();
            if (StrUtil.isBlank(name) || !name.contains(".")) {
                log.warn("{} 文件扩展名不合法，fileId={}, fileName={}", categoryName, file.getId(), name);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
            }
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
            if (!allowedExts.contains(ext)) {
                log.warn("{} 文件类型不允许，fileId={}, ext={}, allowed={}", categoryName, file.getId(), ext, allowedExts);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 批量查询文件并同时校验存在性与扩展名
     * <p>一次 DB 查询替代原先两次查询（先查存在性再查文件信息），
     * 任意文件 ID 不存在或文件类型不在允许集合内均抛出异常。</p>
     *
     * @param fileIds      文件 ID 列表；为空时直接返回空列表
     * @param allowedExts  允许的扩展名集合；为空集合时跳过类型校验，只校验存在性
     * @param categoryName 文件类别名称，用于日志和异常提示
     * @return 查询到的文件 VO 列表，顺序与 fileIds 无关
     * @throws BusinessException 任一文件不存在时抛出 ATTACHMENT_NOT_FOUND；
     *                           任一文件类型不允许时抛出 ATTACHMENT_TYPE_NOT_ALLOWED
     */
    @Override
    public List<FileVO> listAndValidate(List<String> fileIds, Set<String> allowedExts, String categoryName) {
        if (CollUtil.isEmpty(fileIds)) {
            return List.of();
        }
        // 一次查询同时用于存在性校验和类型校验
        List<FileVO> found = listByIds(fileIds);
        Set<String> foundIds = found.stream().map(FileVO::getId).collect(Collectors.toSet());
        for (String fileId : fileIds) {
            if (!foundIds.contains(fileId)) {
                log.warn("{} 文件不存在，fileId={}", categoryName, fileId);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
        }
        // allowedExts 为空时跳过类型校验（调用方明确表示不限制类型）
        if (CollUtil.isNotEmpty(allowedExts)) {
            assertAllFileTypesAllowed(found, allowedExts, categoryName);
        }
        return found;
    }

    /**
     * 校验文件大小是否在允许范围内
     * <p>优先使用 maxSizeMbStr（来自 sys_config），解析失败或为空时使用 fallbackMb。</p>
     *
     * @param fileSizeBytes 文件大小（字节）
     * @param maxSizeMbStr  sys_config 中读取的最大大小字符串（MB），可为 null/blank
     * @param fallbackMb    兜底最大大小（MB），当配置值缺失或格式错误时使用
     * @param categoryName  文件类别名称，用于日志和异常提示
     * @throws BusinessException 文件大小超出限制时抛出 ATTACHMENT_SIZE_EXCEEDED
     */
    @Override
    public void assertFileSizeAllowed(long fileSizeBytes, String maxSizeMbStr, int fallbackMb, String categoryName) {
        int maxMb = fallbackMb;
        if (StrUtil.isNotBlank(maxSizeMbStr)) {
            try {
                maxMb = Integer.parseInt(maxSizeMbStr.trim());
            } catch (NumberFormatException e) {
                log.warn("{} 最大文件大小配置值格式错误，使用兜底值 {}MB，configValue={}",
                        categoryName, fallbackMb, maxSizeMbStr);
            }
        }
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (fileSizeBytes > maxBytes) {
            log.warn("{} 文件大小超出限制，size={}bytes, maxSize={}MB", categoryName, fileSizeBytes, maxMb);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_SIZE_EXCEEDED);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 文件上传核心逻辑
     *
     * <p>执行顺序：</p>
     * <ol>
     *   <li>全局安全校验（大小上限 + 文件名安全）</li>
     *   <li>bizType 合法性校验（必须是 FileBizTypeEnum 中已定义的 dict_code）</li>
     *   <li>按业务类型的 configPrefix 动态校验格式和大小（通过 FileUploadConfigProvider 读 sys_config）</li>
     *   <li>调用 x-file-storage 写入存储平台，FileRecorder 自动持久化记录</li>
     *   <li>从 DB 重新查询完整记录（含自动填充字段）并返回 VO</li>
     * </ol>
     *
     * <p>configPrefix 为 null 的业务类型（如订单其他附件）跳过第 3 步，后缀和大小不受限制。</p>
     *
     * @param file    上传的文件
     * @param bizType 业务类型 dict_code（如 "10.1"）
     * @param bizId   业务 ID，null 表示暂不关联
     * @return 文件 VO
     */
    private FileVO doUpload(MultipartFile file, String bizType, Long bizId) {
        try {
            // 1. 全局文件安全校验（大小上限 + 文件名安全）
            validateFile(file);

            // 2. 校验 bizType 是否为合法的 dict_code
            FileBizTypeEnum fileBizTypeEnum = FileBizTypeEnum.getByDictCode(bizType);
            if (fileBizTypeEnum == null) {
                log.warn("业务类型不合法，bizType={}", bizType);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "bizType");
            }

            // 3. 按业务类型的 configPrefix 动态校验格式和大小
            // configPrefix 为 null 表示该业务类型不限制后缀和大小，跳过此步骤
            String configPrefix = fileBizTypeEnum.getConfigPrefix();
            if (configPrefix != null) {
                // 3.1 文件后缀校验：从 sys_config 读取允许的扩展名白名单
                String allowedExtConfig = fileUploadConfigProvider.getAllowedExtensions(configPrefix);
                if (StrUtil.isNotBlank(allowedExtConfig)) {
                    Set<String> allowedExts = parseAllowedExtensions(allowedExtConfig, null);
                    String originalFilename = file.getOriginalFilename();
                    String ext = (originalFilename != null && originalFilename.contains("."))
                            ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase() : "";
                    if (ext.isEmpty() || !allowedExts.contains(ext)) {
                        log.warn("{} 文件类型不允许上传，ext={}, allowed={}", fileBizTypeEnum.getName(), ext, allowedExts);
                        throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
                    }
                }
                // 3.2 文件大小校验：从 sys_config 读取该业务类型的大小上限
                Integer maxSizeMb = fileUploadConfigProvider.getMaxSizeMb(configPrefix);
                if (maxSizeMb != null) {
                    long maxBytes = (long) maxSizeMb * 1024 * 1024;
                    if (file.getSize() > maxBytes) {
                        log.warn("{} 文件大小超出限制，size={}bytes, maxSize={}MB",
                                fileBizTypeEnum.getName(), file.getSize(), maxSizeMb);
                        throw new BusinessException(ErrorCodeEnum.ATTACHMENT_SIZE_EXCEEDED);
                    }
                }
            }

            // 4. 上传到存储平台
            // 存储路径格式：{bizTypeCode}/{yyyyMM}/，例如 image_data/202604/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String storagePath = fileBizTypeEnum.getCode() + "/" + datePath + "/";
            String objectType = fileBizTypeEnum.getDictCode();
            FileInfo fileInfo = fileStorageService.of(file)
                    .setPath(storagePath)
                    .setObjectType(objectType)
                    .setObjectId(bizId != null ? bizId.toString() : null)
                    .upload();
            log.info("上传文件成功，id={}, bizType={}, url={}", fileInfo.getId(), bizType, fileInfo.getUrl());

            // 5. 从数据库查询完整记录（含 createTime、createBy 等自动填充字段）
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
     * 全局文件安全校验
     * <p>校验内容：文件非空、大小不超全局上限（来自 {@link FileStorageProperties}）、文件名安全性。
     * 不校验扩展名白名单，各业务类型的后缀限制由 {@link FileUploadConfigProvider} 在 {@link #doUpload} 中处理。</p>
     *
     * @param file 待校验文件
     * @throws BusinessException 文件为空、超出全局大小限制或文件名含非法字符
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            log.warn("文件大小超出全局限制，size={}, maxSize={}",
                    file.getSize(), fileStorageProperties.getMaxFileSize());
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_SIZE_EXCEEDED);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_FILENAME_ILLEGAL);
        }
        if (!isValidFilename(originalFilename)) {
            log.warn("文件名包含非法字符，fileName={}", originalFilename);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_FILENAME_ILLEGAL);
        }
    }

    /**
     * 校验文件名是否安全
     * <p>过滤以下危险字符：</p>
     * <ul>
     *   <li>路径遍历：{@code ..}、{@code /}、{@code \}</li>
     *   <li>Windows 非法字符：{@code < > : " | ? *}</li>
     *   <li>ASCII 控制字符（0x00–0x1F 和 0x7F）</li>
     * </ul>
     *
     * @param filename 原始文件名
     * @return true 表示安全，false 表示包含非法字符
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

    /**
     * 将 byte[] 包装为 MultipartFile，供 doUpload 统一处理
     */
    private static class ByteArrayMultipartFile implements MultipartFile {

        private final byte[] content;
        private final String filename;

        ByteArrayMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content == null ? 0 : content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
