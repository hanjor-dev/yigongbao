package com.yigongbao.module.basic.file.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.config.FileStorageProperties;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.provider.FileUploadConfigProvider;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件存储服务单元测试
 *
 * @author hanjor
 * @date 2026-03-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FileService 单元测试")
class FileServiceImplTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileRecorderService fileRecorderService;

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private FileUploadConfigProvider fileUploadConfigProvider;

    @InjectMocks
    private FileServiceImpl fileService;

    private FileDetail testDetail;
    private FileVO testVO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testDetail = new FileDetail();
        testDetail.setId("1926082412345678901");
        testDetail.setUrl("http://localhost:8080/api/files/public/image_data/202603/test.jpg");
        testDetail.setSize(1024L);
        testDetail.setFilename("1926082412345678901.jpg");
        testDetail.setOriginalFilename("test.jpg");
        testDetail.setPath("image_data/202603/");
        testDetail.setExt("jpg");
        testDetail.setContentType("image/jpeg");
        testDetail.setPlatform("local");
        testDetail.setObjectType("10.15");
        testDetail.setObjectId("1");
        testDetail.setCreateTime(now);

        testVO = new FileVO();
        testVO.setId("1926082412345678901");
        testVO.setBizType("10.15");
        testVO.setBizId(1L);
        testVO.setFileName("test.jpg");
        testVO.setFilePath("image_data/202603/");
        testVO.setFileUrl("http://localhost:8080/api/files/public/image_data/202603/test.jpg");
        testVO.setFileSize(1024L);
        testVO.setFileSizeText("1.00 KB");
        testVO.setFileType("image/jpeg");
        testVO.setFileExt("jpg");
        testVO.setPlatform("local");
        testVO.setCreateTime(now);

        when(fileStorageProperties.getMaxFileSize()).thenReturn(524288000L);
    }

    // ==================== uploadFile 测试 ====================

    @Nested
    @DisplayName("uploadFile 测试")
    class UploadFileTests {

        @Test
        @DisplayName("uploadFile: 上传文件成功")
        void uploadFile_shouldSuccess() {
            FileInfo fileInfo = createFileInfo("1926082412345678901", "10.15", null);

            UploadPretreatment pretreatment = mock(UploadPretreatment.class);
            when(fileStorageService.of(any())).thenReturn(pretreatment);
            when(pretreatment.setPath(anyString())).thenReturn(pretreatment);
            when(pretreatment.setObjectType(anyString())).thenReturn(pretreatment);
            when(pretreatment.setObjectId(any())).thenReturn(pretreatment);
            when(pretreatment.upload()).thenReturn(fileInfo);
            when(fileRecorderService.getDetailById("1926082412345678901")).thenReturn(testDetail);
            when(fileRecorderService.toFileVO(testDetail)).thenReturn(testVO);

            org.springframework.mock.web.MockMultipartFile file =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "test.jpg", "image/jpeg", "test content".getBytes());

            FileVO result = fileService.uploadFile(file, "10.15");

            assertNotNull(result);
            assertEquals("1926082412345678901", result.getId());
            assertEquals("test.jpg", result.getFileName());
            verify(fileStorageService).of(any(MultipartFile.class));
            verify(pretreatment).upload();
            verify(fileRecorderService).getDetailById("1926082412345678901");
        }

        @Test
        @DisplayName("uploadAndLink: 业务关联时设置 objectId")
        void uploadAndLink_shouldSetObjectId() {
            FileInfo fileInfo = createFileInfo("1926082412345678902", "10.15", "1");

            UploadPretreatment pretreatment = mock(UploadPretreatment.class);
            when(fileStorageService.of(any())).thenReturn(pretreatment);
            when(pretreatment.setPath(anyString())).thenReturn(pretreatment);
            when(pretreatment.setObjectType(anyString())).thenReturn(pretreatment);
            when(pretreatment.setObjectId(any())).thenReturn(pretreatment);
            when(pretreatment.upload()).thenReturn(fileInfo);
            when(fileRecorderService.getDetailById("1926082412345678902")).thenReturn(testDetail);
            when(fileRecorderService.toFileVO(testDetail)).thenReturn(testVO);

            org.springframework.mock.web.MockMultipartFile file =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "cert.pdf", "application/pdf", "pdf content".getBytes());

            FileVO result = fileService.uploadAndLink(file, "10.15", 1L);

            assertNotNull(result);
            verify(pretreatment).setObjectId("1");
        }
    }

    // ==================== uploadMultiple 测试 ====================

    @Nested
    @DisplayName("uploadMultiple 测试")
    class UploadMultipleTests {

        @Test
        @DisplayName("uploadMultiple: 空数组返回空列表")
        void uploadMultiple_whenEmptyArray_shouldReturnEmptyList() {
            MultipartFile[] emptyFiles = new MultipartFile[0];
            List<FileVO> result = fileService.uploadMultiple(emptyFiles, "10.15");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("uploadMultiple: 多文件上传成功（不关联业务）")
        void uploadMultiple_shouldSuccess() {
            FileInfo fileInfo1 = createFileInfo("1926082412345678903", "10.15", null);
            FileInfo fileInfo2 = createFileInfo("1926082412345678904", "10.15", null);

            UploadPretreatment pretreatment1 = mock(UploadPretreatment.class);
            UploadPretreatment pretreatment2 = mock(UploadPretreatment.class);
            when(fileStorageService.of(any())).thenReturn(pretreatment1).thenReturn(pretreatment2);
            when(pretreatment1.setPath(anyString())).thenReturn(pretreatment1);
            when(pretreatment1.setObjectType(anyString())).thenReturn(pretreatment1);
            when(pretreatment1.setObjectId(any())).thenReturn(pretreatment1);
            when(pretreatment1.upload()).thenReturn(fileInfo1);
            when(pretreatment2.setPath(anyString())).thenReturn(pretreatment2);
            when(pretreatment2.setObjectType(anyString())).thenReturn(pretreatment2);
            when(pretreatment2.setObjectId(any())).thenReturn(pretreatment2);
            when(pretreatment2.upload()).thenReturn(fileInfo2);
            when(fileRecorderService.getDetailById("1926082412345678903")).thenReturn(testDetail);
            when(fileRecorderService.getDetailById("1926082412345678904")).thenReturn(testDetail);
            when(fileRecorderService.toFileVO(any())).thenReturn(testVO);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "test2.jpg", MediaType.IMAGE_JPEG_VALUE, "content2".getBytes());
            MultipartFile[] files = new MultipartFile[]{file1, file2};

            List<FileVO> result = fileService.uploadMultiple(files, "10.15");

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(fileStorageService, times(2)).of(any());
            verify(pretreatment1, times(1)).upload();
            verify(pretreatment2, times(1)).upload();
        }

        @Test
        @DisplayName("uploadMultipleWithBizId: 多文件上传成功（关联业务）")
        void uploadMultipleWithBizId_shouldSuccess() {
            FileInfo fileInfo1 = createFileInfo("1926082412345678905", "10.15", "1");
            FileInfo fileInfo2 = createFileInfo("1926082412345678906", "10.15", "1");

            UploadPretreatment pretreatment1 = mock(UploadPretreatment.class);
            UploadPretreatment pretreatment2 = mock(UploadPretreatment.class);
            when(fileStorageService.of(any())).thenReturn(pretreatment1).thenReturn(pretreatment2);
            when(pretreatment1.setPath(anyString())).thenReturn(pretreatment1);
            when(pretreatment1.setObjectType(anyString())).thenReturn(pretreatment1);
            when(pretreatment1.setObjectId(any())).thenReturn(pretreatment1);
            when(pretreatment1.upload()).thenReturn(fileInfo1);
            when(pretreatment2.setPath(anyString())).thenReturn(pretreatment2);
            when(pretreatment2.setObjectType(anyString())).thenReturn(pretreatment2);
            when(pretreatment2.setObjectId(any())).thenReturn(pretreatment2);
            when(pretreatment2.upload()).thenReturn(fileInfo2);
            when(fileRecorderService.getDetailById("1926082412345678905")).thenReturn(testDetail);
            when(fileRecorderService.getDetailById("1926082412345678906")).thenReturn(testDetail);
            when(fileRecorderService.toFileVO(any())).thenReturn(testVO);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "cert1.pdf", MediaType.APPLICATION_PDF_VALUE, "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "cert2.pdf", MediaType.APPLICATION_PDF_VALUE, "content2".getBytes());
            MultipartFile[] files = new MultipartFile[]{file1, file2};

            List<FileVO> result = fileService.uploadMultipleWithBizId(files, "10.15", 1L);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(fileStorageService, times(2)).of(any());
            verify(pretreatment1, times(1)).upload();
            verify(pretreatment2, times(1)).upload();
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 文件存在时返回 VO")
        void getById_whenExists_shouldReturnVO() {
            when(fileRecorderService.getDetailById("1926082412345678901")).thenReturn(testDetail);
            when(fileRecorderService.toFileVO(testDetail)).thenReturn(testVO);

            FileVO result = fileService.getById("1926082412345678901");

            assertNotNull(result);
            assertEquals("1926082412345678901", result.getId());
            assertEquals("test.jpg", result.getFileName());
        }

        @Test
        @DisplayName("getById: 文件不存在时抛出异常")
        void getById_whenNotExists_shouldThrowException() {
            when(fileRecorderService.getDetailById("not-exists")).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> fileService.getById("not-exists"));
            assertEquals(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== listByBiz 测试 ====================

    @Nested
    @DisplayName("listByBiz 测试")
    class ListByBizTests {

        @Test
        @DisplayName("listByBiz: 有数据时返回列表")
        void listByBiz_whenExists_shouldReturnList() {
            when(fileRecorderService.list(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testDetail));
            when(fileRecorderService.toFileVO(testDetail)).thenReturn(testVO);

            List<FileVO> result = fileService.listByBiz("10.15", 1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("test.jpg", result.get(0).getFileName());
        }

        @Test
        @DisplayName("listByBiz: 无数据时返回空列表")
        void listByBiz_whenNotExists_shouldReturnEmptyList() {
            when(fileRecorderService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<FileVO> result = fileService.listByBiz("10.15", 1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== deleteById 测试 ====================

    @Nested
    @DisplayName("deleteById 测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("deleteById: 删除成功")
        void deleteById_shouldSuccess() {
            when(fileRecorderService.getDetailById("1926082412345678901")).thenReturn(testDetail);
            when(fileStorageService.delete(testDetail.getUrl())).thenReturn(true);
            when(fileRecorderService.removeById("1926082412345678901")).thenReturn(true);

            assertDoesNotThrow(() -> fileService.deleteById("1926082412345678901"));

            verify(fileStorageService).delete(testDetail.getUrl());
            verify(fileRecorderService).removeById("1926082412345678901");
        }

        @Test
        @DisplayName("deleteById: 文件不存在时抛出异常")
        void deleteById_whenNotExists_shouldThrowException() {
            when(fileRecorderService.getDetailById("not-exists")).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> fileService.deleteById("not-exists"));
            assertEquals(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== download 测试 ====================

    @Nested
    @DisplayName("download 测试")
    class DownloadTests {

        @Test
        @DisplayName("download: 指定下载文件名时覆盖响应头名称")
        void download_withOverrideFilename_shouldUseOverrideName() throws Exception {
            FileInfo fileInfo = createFileInfo(testDetail.getId(), testDetail.getObjectType(), testDetail.getObjectId());
            org.dromara.x.file.storage.core.Downloader downloader =
                    mock(org.dromara.x.file.storage.core.Downloader.class);
            when(fileRecorderService.getDetailById(testDetail.getId())).thenReturn(testDetail);
            when(fileRecorderService.getById(testDetail.getId())).thenReturn(fileInfo);
            when(fileStorageService.download(fileInfo)).thenReturn(downloader);
            org.springframework.mock.web.MockHttpServletResponse response =
                    new org.springframework.mock.web.MockHttpServletResponse();

            fileService.download(testDetail.getId(), "零 五-医疗器械图纸.xlsx", response);

            String contentDisposition = response.getHeader("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("filename*="));
            assertTrue(contentDisposition.contains("%E9%9B%B6%20%E4%BA%94-%E5%8C%BB%E7%96%97%E5%99%A8%E6%A2%B0%E5%9B%BE%E7%BA%B8.xlsx"));
            assertFalse(contentDisposition.contains("+"));
        }

        @Test
        @DisplayName("download: 旧签名仍使用持久化原始文件名")
        void download_withoutOverrideFilename_shouldUseOriginalName() throws Exception {
            FileInfo fileInfo = createFileInfo(testDetail.getId(), testDetail.getObjectType(), testDetail.getObjectId());
            org.dromara.x.file.storage.core.Downloader downloader =
                    mock(org.dromara.x.file.storage.core.Downloader.class);
            when(fileRecorderService.getDetailById(testDetail.getId())).thenReturn(testDetail);
            when(fileRecorderService.getById(testDetail.getId())).thenReturn(fileInfo);
            when(fileStorageService.download(fileInfo)).thenReturn(downloader);
            org.springframework.mock.web.MockHttpServletResponse response =
                    new org.springframework.mock.web.MockHttpServletResponse();

            fileService.download(testDetail.getId(), response);

            assertTrue(response.getHeader("Content-Disposition").contains("test.jpg"));
        }

        @Test
        @DisplayName("download: 文件不存在时抛出异常")
        void download_whenNotExists_shouldThrowException() {
            when(fileRecorderService.getDetailById("not-exists")).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> fileService.download("not-exists",
                            new org.springframework.mock.web.MockHttpServletResponse()));

            assertEquals(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== validateFile 私有方法测试 ====================

    @Nested
    @DisplayName("validateFile 私有方法测试")
    class ValidateFileTests {

        @Test
        @DisplayName("validateFile: 空文件抛出异常")
        void validateFile_whenEmpty_shouldThrowException() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("validateFile",
                    MultipartFile.class);
            method.setAccessible(true);

            org.springframework.mock.web.MockMultipartFile emptyFile =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "test.jpg", "image/jpeg", new byte[0]);

            InvocationTargetException ite = assertThrows(
                    InvocationTargetException.class,
                    () -> method.invoke(fileService, emptyFile));
            assertTrue(ite.getCause() instanceof BusinessException);
        }

        @Test
        @DisplayName("validateFile: 不支持的扩展名抛出异常")
        void validateFile_whenBadExtension_shouldThrowException() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("validateFile",
                    MultipartFile.class);
            method.setAccessible(true);

            when(fileStorageProperties.getMaxFileSize()).thenReturn(524288000L);

            org.springframework.mock.web.MockMultipartFile badFile =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "test.exe", "application/octet-stream", "virus".getBytes());

            // validateFile 只校验空文件、大小、文件名安全性，不校验扩展名（扩展名由 bizType 层处理）
            // 此文件名合法，不应抛出异常
            assertDoesNotThrow(() -> {
                try {
                    method.invoke(fileService, badFile);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    if (e.getCause() instanceof BusinessException) {
                        throw (BusinessException) e.getCause();
                    }
                    throw e;
                }
            });
        }
    }

    // ==================== isValidFilename 私有方法测试 ====================

    @Nested
    @DisplayName("isValidFilename 私有方法测试")
    class IsValidFilenameTests {

        @Test
        @DisplayName("isValidFilename: 正常文件名应返回 true")
        void isValidFilename_shouldReturnTrue() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("isValidFilename", String.class);
            method.setAccessible(true);

            assertTrue((Boolean) method.invoke(fileService, "test.jpg"));
            assertTrue((Boolean) method.invoke(fileService, "my_file.pdf"));
            assertTrue((Boolean) method.invoke(fileService, "图片中文.png"));
        }

        @Test
        @DisplayName("isValidFilename: 包含路径遍历应返回 false")
        void isValidFilename_withPathTraversal_shouldReturnFalse() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("isValidFilename", String.class);
            method.setAccessible(true);

            assertFalse((Boolean) method.invoke(fileService, "../test.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "..\\test.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "test/../test.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "C:\\test.jpg"));
        }

        @Test
        @DisplayName("isValidFilename: 包含非法字符应返回 false")
        void isValidFilename_withIllegalChar_shouldReturnFalse() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("isValidFilename", String.class);
            method.setAccessible(true);

            assertFalse((Boolean) method.invoke(fileService, "test<file>.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "test:file.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "test|file.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "test?file.jpg"));
            assertFalse((Boolean) method.invoke(fileService, "test*file.jpg"));
        }

        @Test
        @DisplayName("isValidFilename: null 或空字符串应返回 false")
        void isValidFilename_whenNullOrEmpty_shouldReturnFalse() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("isValidFilename", String.class);
            method.setAccessible(true);

            assertFalse((Boolean) method.invoke(fileService, (String) null));
            assertFalse((Boolean) method.invoke(fileService, ""));
        }
    }

    // ==================== 工具方法 ====================

    private FileInfo createFileInfo(String id, String objectType, String objectId) {
        FileInfo info = new FileInfo();
        info.setId(id);
        info.setUrl("http://localhost:8080/api/files/public/" + objectType + "/202603/" + id + ".jpg");
        info.setSize(1024L);
        info.setFilename(id + ".jpg");
        info.setOriginalFilename("test.jpg");
        info.setPath(objectType + "/202603/");
        info.setExt("jpg");
        info.setContentType("image/jpeg");
        info.setPlatform("local");
        info.setObjectType(objectType);
        info.setObjectId(objectId);
        return info;
    }
}
