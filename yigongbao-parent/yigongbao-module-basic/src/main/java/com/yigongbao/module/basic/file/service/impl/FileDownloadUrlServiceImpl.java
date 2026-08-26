package com.yigongbao.module.basic.file.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.module.basic.file.config.FileStorageProperties;
import com.yigongbao.module.basic.file.service.FileDownloadUrlRequest;
import com.yigongbao.module.basic.file.service.FileDownloadUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * x-file-storage 预签名下载地址实现。
 *
 * <p>文件内容始终由 COS/OSS 直接返回，业务服务只负责生成短时效签名地址。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadUrlServiceImpl implements FileDownloadUrlService {

    private static final String DEFAULT_DOWNLOAD_NAME = "download";

    private final FileStorageService fileStorageService;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public String generate(FileInfo fileInfo, String downloadName) {
        if (fileInfo == null || StrUtil.isBlank(fileInfo.getPlatform())
                || StrUtil.isBlank(fileInfo.getFilename())) {
            return null;
        }

        try {
            Date expiration = Date.from(java.time.Instant.now()
                    .plus(Duration.ofMinutes(fileStorageProperties.getDownloadUrlExpireMinutes())));
            String contentDisposition = buildContentDisposition(downloadName, fileInfo.getOriginalFilename());
            GeneratePresignedUrlResult result = fileStorageService.generatePresignedUrl()
                    .setPlatform(fileInfo.getPlatform())
                    .setPath(fileInfo.getPath())
                    .setFilename(fileInfo.getFilename())
                    .setMethod(Constant.GeneratePresignedUrl.Method.GET)
                    .setExpiration(expiration)
                    .putResponseHeaders(Constant.Metadata.CONTENT_DISPOSITION, contentDisposition)
                    .generatePresignedUrl();
            return result == null ? null : result.getUrl();
        } catch (RuntimeException ex) {
            log.warn("生成文件下载地址失败，fileId={}, platform={}", fileInfo.getId(), fileInfo.getPlatform(), ex);
            return null;
        }
    }

    @Override
    public List<String> generateBatch(List<FileDownloadUrlRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(request -> request == null ? null : generate(request.fileInfo(), request.downloadName()))
                .toList();
    }

    private String buildContentDisposition(String downloadName, String originalFilename) {
        String filename = StrUtil.blankToDefault(downloadName, originalFilename);
        filename = StrUtil.blankToDefault(filename, DEFAULT_DOWNLOAD_NAME);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + buildAsciiFallback(filename) + "\"; filename*=UTF-8''"
                + encodedFilename;
    }

    private String buildAsciiFallback(String filename) {
        if (filename.chars().allMatch(c -> c >= 0x20 && c <= 0x7E)
                && !filename.contains("\\") && !filename.contains("\"")
                && !filename.contains(";")) {
            return filename;
        }
        int extensionIndex = filename.lastIndexOf('.');
        String extension = extensionIndex > 0 ? filename.substring(extensionIndex) : "";
        return DEFAULT_DOWNLOAD_NAME + extension;
    }
}
