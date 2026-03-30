package com.yigongbao.module.basic.file.controller;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件管理 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-25
 */
@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("FileController 接口测试")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    private FileVO buildTestVO(String id, String bizType, Long bizId, String fileName) {
        FileVO vo = new FileVO();
        vo.setId(id);
        vo.setBizType(bizType);
        vo.setBizId(bizId);
        vo.setFileName(fileName);
        // fileUrl 路径前缀使用枚举 code，非 dictCode
        vo.setFilePath("image_data/202603/" + fileName);
        vo.setFileUrl("http://localhost:8080/api/files/public/image_data/202603/" + fileName);
        vo.setFileSize(1024L);
        vo.setFileType("image/jpeg");
        vo.setFileExt("jpg");
        vo.setPlatform("local");
        vo.setFileSizeText("1.00 KB");
        vo.setCreateTime(java.time.LocalDateTime.now());
        return vo;
    }

    // ==================== upload 测试 ====================

    @Nested
    @DisplayName("upload 测试")
    class UploadTests {

        @Test
        @DisplayName("upload: 上传文件成功")
        void upload_shouldSuccess() throws Exception {
            FileVO vo = buildTestVO("1926082412345678901", "10.15", null, "test.jpg");
            when(fileService.uploadFile(any(), eq("10.15"))).thenReturn(vo);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

            mockMvc.perform(multipart("/basic/file/upload")
                            .file(file)
                            .param("bizType", "10.15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.id").value("1926082412345678901"))
                    .andExpect(jsonPath("$.data.fileName").value("test.jpg"));

            verify(fileService, times(1)).uploadFile(any(), eq("10.15"));
        }

        @Test
        @DisplayName("upload: bizType 为空时返回参数错误")
        void upload_whenBizTypeBlank_shouldReturnBadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());

            mockMvc.perform(multipart("/basic/file/upload")
                            .file(file))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== uploadAndLink 测试 ====================

    @Nested
    @DisplayName("uploadAndLink 测试")
    class UploadAndLinkTests {

        @Test
        @DisplayName("uploadAndLink: 上传并关联业务成功")
        void uploadAndLink_shouldSuccess() throws Exception {
            FileVO vo = buildTestVO("1926082412345678902", "10.15", 1L, "cert.pdf");
            when(fileService.uploadAndLink(any(), eq("10.15"), eq(1L))).thenReturn(vo);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "cert.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf content".getBytes());

            mockMvc.perform(multipart("/basic/file/upload-and-link")
                            .file(file)
                            .param("bizType", "10.15")
                            .param("bizId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.id").value("1926082412345678902"))
                    .andExpect(jsonPath("$.data.bizType").value("10.15"))
                    .andExpect(jsonPath("$.data.bizId").value(1));

            verify(fileService, times(1)).uploadAndLink(any(), eq("10.15"), eq(1L));
        }

        @Test
        @DisplayName("uploadAndLink: bizId 为空时返回参数错误")
        void uploadAndLink_whenBizIdNull_shouldReturnBadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cert.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf content".getBytes());

            mockMvc.perform(multipart("/basic/file/upload-and-link")
                            .file(file)
                            .param("bizType", "10.15"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== uploadMultiple 测试 ====================

    @Nested
    @DisplayName("uploadMultiple 测试")
    class UploadMultipleTests {

        @Test
        @DisplayName("uploadMultiple: 批量上传成功")
        void uploadMultiple_shouldSuccess() throws Exception {
            List<FileVO> vos = Arrays.asList(
                    buildTestVO("1926082412345678903", "10.15", 1L, "test1.jpg"),
                    buildTestVO("1926082412345678904", "10.15", 1L, "test2.jpg"));
            when(fileService.uploadMultiple(any(), eq("10.15"), eq(1L))).thenReturn(vos);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "test2.jpg", MediaType.IMAGE_JPEG_VALUE, "content2".getBytes());

            mockMvc.perform(multipart("/basic/file/upload-multiple")
                            .file(file1)
                            .file(file2)
                            .param("bizType", "10.15")
                            .param("bizId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));

            verify(fileService, times(1)).uploadMultiple(any(), eq("10.15"), eq(1L));
        }
    }

    // ==================== listByBiz 测试 ====================

    @Nested
    @DisplayName("listByBiz 测试")
    class ListByBizTests {

        @Test
        @DisplayName("listByBiz: 存在文件时返回列表")
        void listByBiz_whenExists_shouldReturnList() throws Exception {
            List<FileVO> vos = Arrays.asList(
                    buildTestVO("1926082412345678905", "10.15", 1L, "cert1.pdf"),
                    buildTestVO("1926082412345678906", "10.15", 1L, "cert2.pdf"));
            when(fileService.listByBiz("10.15", 1L)).thenReturn(vos);

            mockMvc.perform(get("/basic/file/list/10.15/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].fileName").value("cert1.pdf"));

            verify(fileService, times(1)).listByBiz("10.15", 1L);
        }

        @Test
        @DisplayName("listByBiz: 无文件时返回空列表")
        void listByBiz_whenNotExists_shouldReturnEmptyList() throws Exception {
            when(fileService.listByBiz("10.15", 1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/basic/file/list/10.15/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(fileService, times(1)).listByBiz("10.15", 1L);
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 文件存在时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            FileVO vo = buildTestVO("1926082412345678907", "10.15", 1L, "test.jpg");
            when(fileService.getById("1926082412345678907")).thenReturn(vo);

            mockMvc.perform(get("/basic/file/1926082412345678907"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value("1926082412345678907"))
                    .andExpect(jsonPath("$.data.fileName").value("test.jpg"));

            verify(fileService, times(1)).getById("1926082412345678907");
        }

        @Test
        @DisplayName("getById: 文件不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(fileService.getById("not-exists-id"))
                    .thenThrow(new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND));

            mockMvc.perform(get("/basic/file/not-exists-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode()));
        }
    }

    // ==================== deleteById 测试 ====================

    @Nested
    @DisplayName("deleteById 测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("deleteById: 删除文件成功")
        void deleteById_shouldSuccess() throws Exception {
            doNothing().when(fileService).deleteById("1926082412345678908");

            mockMvc.perform(delete("/basic/file/1926082412345678908"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));

            verify(fileService, times(1)).deleteById("1926082412345678908");
        }

        @Test
        @DisplayName("deleteById: 文件不存在时返回错误")
        void deleteById_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND))
                    .when(fileService).deleteById("not-exists-id");

            mockMvc.perform(delete("/basic/file/not-exists-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode()));
        }
    }
}
