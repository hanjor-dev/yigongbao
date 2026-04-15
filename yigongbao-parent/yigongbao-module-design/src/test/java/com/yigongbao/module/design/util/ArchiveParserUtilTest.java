package com.yigongbao.module.design.util;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.design.dto.ArchiveFileInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveParserUtil 单元测试
 *
 * @author hanjor
 * @date 2026-04-15
 */
@DisplayName("ArchiveParserUtil 单元测试")
class ArchiveParserUtilTest {

    @Nested
    @DisplayName("isSupported 测试")
    class IsSupportedTest {

        @Test
        @DisplayName("支持 ZIP 格式")
        void shouldSupportZip() {
            assertTrue(ArchiveParserUtil.isSupported("test.zip"));
            assertTrue(ArchiveParserUtil.isSupported("test.ZIP"));
            assertTrue(ArchiveParserUtil.isSupported("path/to/test.zip"));
        }

        @Test
        @DisplayName("支持 RAR 格式")
        void shouldSupportRar() {
            assertTrue(ArchiveParserUtil.isSupported("test.rar"));
            assertTrue(ArchiveParserUtil.isSupported("test.RAR"));
        }

        @Test
        @DisplayName("支持 7Z 格式")
        void shouldSupport7z() {
            assertTrue(ArchiveParserUtil.isSupported("test.7z"));
            assertTrue(ArchiveParserUtil.isSupported("test.7Z"));
        }

        @Test
        @DisplayName("不支持其他格式")
        void shouldNotSupportOtherFormats() {
            assertFalse(ArchiveParserUtil.isSupported("test.tar"));
            assertFalse(ArchiveParserUtil.isSupported("test.gz"));
            assertFalse(ArchiveParserUtil.isSupported("test.txt"));
            assertFalse(ArchiveParserUtil.isSupported("test.stl"));
        }

        @Test
        @DisplayName("空文件名返回 false")
        void shouldReturnFalseForEmptyFileName() {
            assertFalse(ArchiveParserUtil.isSupported(null));
            assertFalse(ArchiveParserUtil.isSupported(""));
            assertFalse(ArchiveParserUtil.isSupported("   "));
        }

        @Test
        @DisplayName("无扩展名返回 false")
        void shouldReturnFalseForNoExtension() {
            assertFalse(ArchiveParserUtil.isSupported("filename"));
        }
    }

    @Nested
    @DisplayName("parse ZIP 测试")
    class ParseZipTest {

        @Test
        @DisplayName("正确解析 ZIP 文件列表")
        void shouldParseZipCorrectly() throws Exception {
            // 创建测试 ZIP 文件
            byte[] zipData = createTestZip(List.of(
                    "model1.stl",
                    "model2.obj",
                    "subdir/model3.stl",
                    "readme.txt"
            ));

            InputStream inputStream = new ByteArrayInputStream(zipData);
            Set<String> allowedExtensions = Set.of(".stl", ".obj");

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", allowedExtensions);

            // 验证过滤后只有 3 个文件（排除 .txt）
            assertEquals(3, result.size());

            // 验证文件信息
            assertTrue(result.stream().anyMatch(f -> f.getFileName().equals("model1.stl")));
            assertTrue(result.stream().anyMatch(f -> f.getFileName().equals("model2.obj")));
            assertTrue(result.stream().anyMatch(f -> f.getFileName().equals("model3.stl")));

            // 验证扩展名
            assertTrue(result.stream().allMatch(f ->
                    f.getExtension().equals(".stl") || f.getExtension().equals(".obj")));
        }

        @Test
        @DisplayName("无过滤条件时返回所有文件")
        void shouldReturnAllFilesWhenNoFilter() throws Exception {
            byte[] zipData = createTestZip(List.of("model1.stl", "readme.txt"));
            InputStream inputStream = new ByteArrayInputStream(zipData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("空过滤条件时返回所有文件")
        void shouldReturnAllFilesWhenEmptyFilter() throws Exception {
            byte[] zipData = createTestZip(List.of("model1.stl", "readme.txt"));
            InputStream inputStream = new ByteArrayInputStream(zipData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", Set.of());

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("跳过目录项")
        void shouldSkipDirectories() throws Exception {
            // 创建包含目录的 ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // 添加目录
                ZipEntry dirEntry = new ZipEntry("subdir/");
                zos.putNextEntry(dirEntry);
                zos.closeEntry();

                // 添加文件
                ZipEntry fileEntry = new ZipEntry("model.stl");
                zos.putNextEntry(fileEntry);
                zos.write("content".getBytes());
                zos.closeEntry();
            }

            InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", null);

            // 只有文件，没有目录
            assertEquals(1, result.size());
            assertEquals("model.stl", result.get(0).getFileName());
        }

        @Test
        @DisplayName("正确提取文件路径中的文件名")
        void shouldExtractFileNameFromPath() throws Exception {
            byte[] zipData = createTestZip(List.of("dir1/dir2/model.stl"));
            InputStream inputStream = new ByteArrayInputStream(zipData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", null);

            assertEquals(1, result.size());
            assertEquals("model.stl", result.get(0).getFileName());
            assertEquals("dir1/dir2/model.stl", result.get(0).getFilePath());
        }
    }

    @Nested
    @DisplayName("parse 异常测试")
    class ParseExceptionTest {

        @Test
        @DisplayName("不支持的格式抛出异常")
        void shouldThrowExceptionForUnsupportedFormat() {
            InputStream inputStream = new ByteArrayInputStream(new byte[0]);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ArchiveParserUtil.parse(inputStream, "test.tar", null));

            assertEquals(736, exception.getCode()); // DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED
        }

        @Test
        @DisplayName("无扩展名抛出异常")
        void shouldThrowExceptionForNoExtension() {
            InputStream inputStream = new ByteArrayInputStream(new byte[0]);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ArchiveParserUtil.parse(inputStream, "filename", null));

            assertEquals(736, exception.getCode());
        }

        @Test
        @DisplayName("损坏的 ZIP 文件返回空列表")
        void shouldReturnEmptyListForCorruptedZip() {
            // 无效的 ZIP 数据不会抛出异常，而是返回空列表
            // 因为 ZipInputStream 遍历时找不到有效 entry
            byte[] corruptedData = "not a valid zip".getBytes();
            InputStream inputStream = new ByteArrayInputStream(corruptedData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", null);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("扩展名处理测试")
    class ExtensionTest {

        @Test
        @DisplayName("扩展名转为小写")
        void shouldConvertExtensionToLowerCase() throws Exception {
            byte[] zipData = createTestZip(List.of("MODEL.STL", "model.OBJ"));
            InputStream inputStream = new ByteArrayInputStream(zipData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", null);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(f ->
                    f.getExtension().equals(".stl") || f.getExtension().equals(".obj")));
        }

        @Test
        @DisplayName("大小写不敏感的过滤")
        void shouldFilterCaseInsensitively() throws Exception {
            byte[] zipData = createTestZip(List.of("MODEL.STL", "model.stl"));
            InputStream inputStream = new ByteArrayInputStream(zipData);

            List<ArchiveFileInfo> result = ArchiveParserUtil.parse(inputStream, "test.zip", Set.of(".stl"));

            assertEquals(2, result.size());
        }
    }

    /**
     * 创建测试用的 ZIP 文件
     */
    private byte[] createTestZip(List<String> fileNames) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String fileName : fileNames) {
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(("content of " + fileName).getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
