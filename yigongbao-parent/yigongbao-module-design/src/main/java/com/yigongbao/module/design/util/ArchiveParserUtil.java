package com.yigongbao.module.design.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.design.dto.ArchiveFileInfo;
import lombok.extern.slf4j.Slf4j;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
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
 * 支持 ZIP/RAR/7Z/TAR 格式，读取文件列表元数据及文件内容，不解压到磁盘
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
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".zip", ".rar", ".7z", ".tar");

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
                case "tar" -> parseTar(inputStream, allowedExtensions);
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
     * 解析 ZIP 格式（同时读取文件内容，用于后续独立上传 OSS）
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
                    // 跳过此条目，但需要消费流，否则 getNextEntry 无法推进
                    zis.transferTo(java.io.OutputStream.nullOutputStream());
                    continue;
                }

                // 读取文件内容（不调用 closeEntry，ZipInputStream.readAllBytes 不会关闭外层流）
                byte[] content = zis.readAllBytes();
                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(entry.getSize() >= 0 ? entry.getSize() : (long) content.length)
                        .extension(ext)
                        .fileContent(content)
                        .build());
            }
        }
        return result;
    }

    /**
     * 解析 RAR 格式（支持 RAR 4.x 和 RAR 5.0）
     */
    private static List<ArchiveFileInfo> parseRar(InputStream inputStream,
                                                   Set<String> allowedExtensions) throws Exception {
        byte[] bytes = inputStream.readAllBytes();

        try {
            return parseRarWithJunrar(bytes, allowedExtensions);
        } catch (com.github.junrar.exception.UnsupportedRarV5Exception e) {
            log.info("检测到 RAR 5.0 格式，使用 sevenzipjbinding 解析");
            return parseRarWithSevenZip(bytes, allowedExtensions);
        }
    }

    /**
     * 使用 junrar 解析 RAR 4.x
     */
    private static List<ArchiveFileInfo> parseRarWithJunrar(byte[] bytes,
                                                             Set<String> allowedExtensions) throws Exception {
        List<ArchiveFileInfo> result = new ArrayList<>();
        try (Archive archive = new Archive(new java.io.ByteArrayInputStream(bytes))) {
            for (FileHeader header : archive) {
                if (header.isDirectory()) {
                    continue;
                }
                String filePath = header.getFileName().replace('\\', '/');
                String entryFileName = extractFileName(filePath);
                String ext = getExtension(entryFileName);

                if (allowedExtensions != null && !allowedExtensions.isEmpty()
                        && !allowedExtensions.contains(ext)) {
                    continue;
                }

                ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                archive.extractFile(header, entryBaos);
                byte[] content = entryBaos.toByteArray();

                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(header.getFullUnpackSize())
                        .extension(ext)
                        .fileContent(content)
                        .build());
            }
        }
        return result;
    }

    /**
     * 使用 sevenzipjbinding 解析 RAR 5.0
     */
    private static List<ArchiveFileInfo> parseRarWithSevenZip(byte[] bytes,
                                                               Set<String> allowedExtensions) throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("rar5_", ".rar");
        try {
            java.nio.file.Files.write(tempFile.toPath(), bytes);
            List<ArchiveFileInfo> result = new ArrayList<>();

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(tempFile, "r");
                 IInArchive archive = SevenZip.openInArchive(null, new RandomAccessFileInStream(raf))) {

                int[] indices = new int[archive.getNumberOfItems()];
                for (int i = 0; i < indices.length; i++) {
                    indices[i] = i;
                }

                archive.extract(indices, false, new IArchiveExtractCallback() {
                    private int currentIndex;
                    private ByteArrayOutputStream outputStream;

                    @Override
                    public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) {
                        currentIndex = index;
                        try {
                            Boolean isFolder = (Boolean) archive.getProperty(index, PropID.IS_FOLDER);
                            if (isFolder != null && isFolder) {
                                return null;
                            }

                            String path = (String) archive.getProperty(index, PropID.PATH);
                            if (path == null) {
                                return null;
                            }

                            String filePath = path.replace('\\', '/');
                            String fileName = extractFileName(filePath);
                            String ext = getExtension(fileName);

                            if (allowedExtensions != null && !allowedExtensions.isEmpty()
                                    && !allowedExtensions.contains(ext)) {
                                return null;
                            }

                            outputStream = new ByteArrayOutputStream();
                            return data -> {
                                try {
                                    outputStream.write(data);
                                    return data.length;
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            };
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void prepareOperation(ExtractAskMode extractAskMode) {
                    }

                    @Override
                    public void setOperationResult(ExtractOperationResult extractOperationResult) {
                        if (outputStream != null && extractOperationResult == ExtractOperationResult.OK) {
                            try {
                                String path = (String) archive.getProperty(currentIndex, PropID.PATH);
                                String filePath = path.replace('\\', '/');
                                String fileName = extractFileName(filePath);
                                Long size = (Long) archive.getProperty(currentIndex, PropID.SIZE);

                                result.add(ArchiveFileInfo.builder()
                                        .fileName(fileName)
                                        .filePath(filePath)
                                        .fileSize(size != null ? size : (long) outputStream.size())
                                        .extension(getExtension(fileName))
                                        .fileContent(outputStream.toByteArray())
                                        .build());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        outputStream = null;
                    }

                    @Override
                    public void setTotal(long total) {
                    }

                    @Override
                    public void setCompleted(long complete) {
                    }
                });
            }
            return result;
        } finally {
            FileUtil.del(tempFile);
        }
    }

    /**
     * 解析 7Z 格式（同时读取文件内容，用于后续独立上传 OSS）
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
                    // 跳过此条目，但需消费内容，否则流可能状态异常
                    sevenZFile.getInputStream(entry).transferTo(java.io.OutputStream.nullOutputStream());
                    continue;
                }

                // 读取文件内容
                byte[] content = sevenZFile.getInputStream(entry).readAllBytes();
                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(entry.getSize())
                        .extension(ext)
                        .fileContent(content)
                        .build());
            }
        }
        return result;
    }

    /**
     * 解析 TAR 格式（同时读取文件内容，用于后续独立上传 OSS）
     */
    private static List<ArchiveFileInfo> parseTar(InputStream inputStream,
                                                   Set<String> allowedExtensions) throws IOException {
        List<ArchiveFileInfo> result = new ArrayList<>();
        try (TarArchiveInputStream tis = new TarArchiveInputStream(inputStream)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
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
                    // 跳过此条目，消费流内容以推进 TAR 读取位置
                    tis.transferTo(java.io.OutputStream.nullOutputStream());
                    continue;
                }

                // 读取文件内容
                byte[] content = tis.readAllBytes();
                result.add(ArchiveFileInfo.builder()
                        .fileName(entryFileName)
                        .filePath(filePath)
                        .fileSize(entry.getRealSize())
                        .extension(ext)
                        .fileContent(content)
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
