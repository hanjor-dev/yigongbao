package com.yigongbao.module.basic.file.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.config.FileStorageProperties;
import com.yigongbao.module.basic.file.entity.FileDetail;
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

    @InjectMocks
    private FileServiceImpl fileService;

    private FileDetail testDetail;
    private FileVO testVO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testDetail = new FileDetail();
        testDetail.setId("1926082412345678901");
        testDetail.setUrl("http://localhost:8080/api/files/registration_cert/202603/test.jpg");
        testDetail.setSize(1024L);
        testDetail.setFilename("1926082412345678901.jpg");
        testDetail.setOriginalFilename("test.jpg");
        testDetail.setPath("registration_cert/202603/");
        testDetail.setExt("jpg");
        testDetail.setContentType("image/jpeg");
        testDetail.setPlatform("local");
        testDetail.setObjectType("registration_cert");
        testDetail.setObjectId("1");
        testDetail.setCreateTime(now);

        testVO = new FileVO();
        testVO.setId("1926082412345678901");
        testVO.setBizType("registration_cert");
        testVO.setBizId(1L);
        testVO.setFileName("test.jpg");
        testVO.setFilePath("registration_cert/202603/");
        testVO.setFileUrl("http://localhost:8080/api/files/registration_cert/202603/test.jpg");
        testVO.setFileSize(1024L);
        testVO.setFileSizeText("1.00 KB");
        testVO.setFileType("image/jpeg");
        testVO.setFileExt("jpg");
        testVO.setPlatform("local");
        testVO.setCreateTime(now);

        when(fileStorageProperties.getMaxFileSize()).thenReturn(524288000L);
        when(fileStorageProperties.getAllowedExtensions()).thenReturn(
                new String[]{"jpg", "jpeg", "png", "pdf", "doc", "docx"});
    }

    // ==================== uploadFile 测试 ====================

    @Nested
    @DisplayName("uploadFile 测试")
    class UploadFileTests {

        @Test
        @DisplayName("uploadFile: 上传文件成功")
        void uploadFile_shouldSuccess() {
            FileInfo fileInfo = createFileInfo("1926082412345678901", "registration_cert", null);

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

            FileVO result = fileService.uploadFile(file, "registration_cert");

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
            FileInfo fileInfo = createFileInfo("1926082412345678902", "registration_cert", "1");

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

            FileVO result = fileService.uploadAndLink(file, "registration_cert", 1L);

            assertNotNull(result);
            verify(pretreatment).setObjectId("1");
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

            List<FileVO> result = fileService.listByBiz("registration_cert", 1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("test.jpg", result.get(0).getFileName());
        }

        @Test
        @DisplayName("listByBiz: 无数据时返回空列表")
        void listByBiz_whenNotExists_shouldReturnEmptyList() {
            when(fileRecorderService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<FileVO> result = fileService.listByBiz("registration_cert", 1L);

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

            when(fileStorageProperties.getAllowedExtensions()).thenReturn(
                    new String[]{"jpg", "png"});
            when(fileStorageProperties.getMaxFileSize()).thenReturn(524288000L);
            FileServiceImpl svc = new FileServiceImpl(
                    fileStorageService, fileRecorderService, fileStorageProperties);

            org.springframework.mock.web.MockMultipartFile badFile =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "test.exe", "application/octet-stream", "virus".getBytes());

            InvocationTargetException ite = assertThrows(
                    InvocationTargetException.class,
                    () -> method.invoke(svc, badFile));
            assertTrue(ite.getCause() instanceof BusinessException);
        }
    }

    // ==================== getFileExt 私有方法测试 ====================

    @Nested
    @DisplayName("getFileExt 私有方法测试")
    class GetFileExtTests {

        @Test
        @DisplayName("getFileExt: 正常文件名应返回扩展名")
        void getFileExt_shouldReturnExtension() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("getFileExt", String.class);
            method.setAccessible(true);

            assertEquals("jpg", method.invoke(fileService, "test.jpg"));
            assertEquals("png", method.invoke(fileService, "test.file.png"));
            assertEquals("jpeg", method.invoke(fileService, "test.JPEG"));
        }

        @Test
        @DisplayName("getFileExt: 无扩展名应返回空字符串")
        void getFileExt_whenNoExtension_shouldReturnEmpty() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("getFileExt", String.class);
            method.setAccessible(true);

            assertEquals("", method.invoke(fileService, "testfile"));
        }

        @Test
        @DisplayName("getFileExt: null 应返回空字符串")
        void getFileExt_whenNull_shouldReturnEmpty() throws Exception {
            Method method = FileServiceImpl.class.getDeclaredMethod("getFileExt", String.class);
            method.setAccessible(true);

            assertEquals("", method.invoke(fileService, (String) null));
        }
    }

    // ==================== 工具方法 ====================

    private FileInfo createFileInfo(String id, String objectType, String objectId) {
        FileInfo info = new FileInfo();
        info.setId(id);
        info.setUrl("http://localhost:8080/api/files/" + objectType + "/202603/" + id + ".jpg");
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
