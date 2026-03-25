package com.yigongbao.module.basic.registrationCert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.registrationCert.RegistrationCertTestApplication;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = RegistrationCertTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RegistrationCertController 接口测试")
class RegistrationCertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationCertService registrationCertService;

    private RegistrationCertVO buildTestVO(Long id, String certCode) {
        RegistrationCertVO vo = new RegistrationCertVO();
        vo.setId(id);
        vo.setCertCode(certCode);
        vo.setCertName("注册证名称");
        vo.setValidFrom(LocalDate.of(2026, 1, 1));
        vo.setValidTo(LocalDate.of(2028, 12, 31));
        vo.setStatus(1);
        vo.setStatusName("有效");
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== list 测试 ====================

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("list: 分页查询成功")
        void list_shouldReturnPageData() throws Exception {
            RegistrationCertVO vo = buildTestVO(1L, "REG-001");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<RegistrationCertVO>(1, 10);
            page.setRecords(List.of(vo));
            page.setTotal(1);
            when(registrationCertService.listCerts(eq(1), eq(10), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/registration-cert/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.records[0].certCode").value("REG-001"));
        }

        @Test
        @DisplayName("list: 空数据时返回空分页")
        void list_whenEmpty_shouldReturnEmptyPage() throws Exception {
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<RegistrationCertVO>(1, 10);
            page.setRecords(new ArrayList<>());
            page.setTotal(0);
            when(registrationCertService.listCerts(eq(1), eq(10), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/registration-cert/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records").isEmpty());
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(registrationCertService.getById(1L)).thenReturn(buildTestVO(1L, "REG-001"));

            mockMvc.perform(get("/api/basic/registration-cert/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.certCode").value("REG-001"));
        }

        @Test
        @DisplayName("getById: 不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(registrationCertService.getById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND));

            mockMvc.perform(get("/api/basic/registration-cert/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(720))
                    .andExpect(jsonPath("$.message").value("注册证不存在"));
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 测试")
    class CreateTests {

        @Test
        @DisplayName("create: 创建成功返回200")
        void create_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "certCode", "REG-NEW001",
                    "certName", "新注册证",
                    "validFrom", "2026-01-01",
                    "validTo", "2030-12-31"
            );

            mockMvc.perform(post("/api/basic/registration-cert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("create: 缺少必填参数时返回400")
        void create_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "validFrom", "2026-01-01"
            );

            mockMvc.perform(post("/api/basic/registration-cert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("create: 编码已存在时返回错误")
        void create_whenCodeExists_shouldReturnError() throws Exception {
            // 模拟 Service 层抛出注册证已存在异常
            doThrow(new BusinessException(ErrorCodeEnum.CERT_EXISTS))
                    .when(registrationCertService).create(any());

            Map<String, Object> requestBody = Map.of(
                    "certCode", "REG-20230002",
                    "certName", "新注册证"
            );

            mockMvc.perform(post("/api/basic/registration-cert")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(721));
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 测试")
    class UpdateTests {

        @Test
        @DisplayName("update: 更新成功返回200")
        void update_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "certName", "更新后的注册证名称"
            );

            mockMvc.perform(put("/api/basic/registration-cert/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND))
                    .when(registrationCertService).update(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "certName", "更新后的注册证名称"
            );

            mockMvc.perform(put("/api/basic/registration-cert/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(720))
                    .andExpect(jsonPath("$.message").value("注册证不存在"));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("delete: 删除成功返回200")
        void delete_shouldSuccess() throws Exception {
            mockMvc.perform(delete("/api/basic/registration-cert/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("delete: 数据不存在时返回错误")
        void delete_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND))
                    .when(registrationCertService).remove(999L);

            mockMvc.perform(delete("/api/basic/registration-cert/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(720));
        }
    }

    // ==================== valid-list 测试 ====================

    @Nested
    @DisplayName("valid-list 测试")
    class ValidListTests {

        @Test
        @DisplayName("valid-list: 返回有效注册证列表")
        void validList_shouldReturnValidList() throws Exception {
            when(registrationCertService.listValidCerts())
                    .thenReturn(List.of(buildTestVO(1L, "REG-001"), buildTestVO(2L, "REG-002")));

            mockMvc.perform(get("/api/basic/registration-cert/valid-list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].certCode").value("REG-001"));
        }
    }
}
