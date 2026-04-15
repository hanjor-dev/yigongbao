package com.yigongbao.module.design.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.design.dto.ArchiveFileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 压缩包解析工具类
 * 支持 ZIP/RAR/7Z 格式，仅读取文件列表元数据，不解压到磁盘
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Slf4j
public final class ArchiveParserUtil {

    private ArchiveParserUtil() {
    }

    /**
     * 支持的压缩包扩展名
     */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".zip", ".rar", ".7z");

    /**
     * 判断是否为支持的压缩包格式
     *
     * @param fileName 文件名
     * @return true=支持
     */
    public static boolean isSupported(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return false;
        }
        String ext = FileUtil.extName(fileName);
        return StrUtil.isNotBlank(ext) && SUPPORTED_EXTENSIONS.contains("." + ext.toLowerCase());
    }

    /**
     * 解析压缩包，返回文件列表
     *
     * @param inputStream       压缩包输入流
     * @param fileName          文件名（用于判断格式）
     * @param allowedExtensions 允许的扩展名集合（小写，含点号，如 .stl）
     * @return 文件信息列表
     */
    public static List<ArchiveFileInfo> parse(InputStream inputStream, String fileName,
                                               Set<String> allowedExtensions) {
        String ext = FileUtil.extName(fileName);
        if (StrUtil.isBlank(ext)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED);
        }
        ext = ext.toLowerCase();

        try {
            return switch (ext) {
                case "zip" -> parseZip(inputStream, allowedExtensions);
                case "rar" -> parseRar(inputStream, allowedExtensions);
                case "7z" -> parse7z(inputStream, allowedExtensions);
                default -> throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析压缩包失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
        }
    }

    /**
     * 解析 ZIP 格式
     */
    private static List<ArchiveFileInfo> parseZip(InputStream inputStream,
                                                   Set<String> allowedExtensions) throws IOException {
        List<ArchiveFileInfo> result = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }
                String filePath = entry.getName();
                String entryFileName = extractFileName(filePath);
                String ext = getExtension(entryFileName);

                // 按扩展名过滤
                if (allowedExtensions != null && !allowedExtensions.isEmpty()
                        && !allowedExtensions.contains(ext)) {
                    continue;
                }

                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(entry.getSize())
                        .extension(ext)
                        .build());
            }
        }
        return result;
    }

    /**
     * 解析 RAR 格式
     */
    private static List<ArchiveFileInfo> parseRar(InputStream inputStream,
                                                   Set<String> allowedExtensions) throws Exception {
        List<ArchiveFileInfo> result = new ArrayList<>();
        // junrar 需要读取完整流到内存
        byte[] bytes = inputStream.readAllBytes();
        try (Archive archive = new Archive(new java.io.ByteArrayInputStream(bytes))) {
            for (FileHeader header : archive) {
                // 跳过目录
                if (header.isDirectory()) {
                    continue;
                }
                String filePath = header.getFileName();
                // RAR 可能使用反斜杠
                filePath = filePath.replace('\\', '/');
                String entryFileName = extractFileName(filePath);
                String ext = getExtension(entryFileName);

                // 按扩展名过滤
                if (allowedExtensions != null && !allowedExtensions.isEmpty()
                        && !allowedExtensions.contains(ext)) {
                    continue;
                }

                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(header.getFullUnpackSize())
                        .extension(ext)
                        .build());
            }
        }
        return result;
    }

    /**
     * 解析 7Z 格式
     */
    private static List<ArchiveFileInfo> parse7z(InputStream inputStream,
                                                  Set<String> allowedExtensions) throws Exception {
        List<ArchiveFileInfo> result = new ArrayList<>();
        // 7Z 需要 SeekableByteChannel，先读取到内存
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        inputStream.transferTo(baos);
        byte[] bytes = baos.toByteArray();

        try (SevenZFile sevenZFile = new SevenZFile(new SeekableInMemoryByteChannel(bytes))) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }
                String filePath = entry.getName();
                String entryFileName = extractFileName(filePath);
                String ext = getExtension(entryFileName);

                // 按扩展名过滤
                if (allowedExtensions != null && !allowedExtensions.isEmpty()
                        && !allowedExtensions.contains(ext)) {
                    continue;
                }

                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(entry.getSize())
                        .extension(ext)
                        .build());
            }
        }
        return result;
    }

    /**
     * 从路径中提取文件名
     */
    private static String extractFileName(String path) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 获取扩展名（小写，含点号）
     */
    private static String getExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase();
    }
}
