package com.yigongbao.module.basic.doctor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.service.DoctorService;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
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

@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DoctorController 接口测试")
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorService doctorService;

    private DoctorVO buildTestVO(Long id, String name) {
        DoctorVO vo = new DoctorVO();
        vo.setId(id);
        vo.setDoctorName(name);
        vo.setDoctorPhone("13800138" + String.format("%03d", id));
        vo.setHospitalId(1L);
        vo.setHospitalName("测试医院");
        vo.setHospitalDeptId(1L);
        vo.setHospitalDeptName("骨科");
        vo.setCreatorId(1L);
        vo.setOrderCount(0);
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== page 测试 ====================

    @Nested
    @DisplayName("page 测试")
    class PageTests {

        @Test
        @DisplayName("page: 分页查询成功")
        void page_shouldReturnPageData() throws Exception {
            DoctorVO vo = buildTestVO(1L, "张三");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<DoctorVO>(1, 10);
            page.setRecords(List.of(vo));
            page.setTotal(1);
            when(doctorService.listDoctors(eq(1), eq(10), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/doctor/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.records[0].doctorName").value("张三"));
        }

        @Test
        @DisplayName("page: 空数据时返回空分页")
        void page_whenEmpty_shouldReturnEmptyPage() throws Exception {
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<DoctorVO>(1, 10);
            page.setRecords(new ArrayList<>());
            page.setTotal(0);
            when(doctorService.listDoctors(eq(1), eq(10), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/doctor/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records").isEmpty());
        }
    }

    // ==================== list 测试 ====================

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("list: 返回所有医生")
        void list_shouldReturnAll() throws Exception {
            when(doctorService.listAll(any(), any(), any()))
                    .thenReturn(List.of(buildTestVO(1L, "张三"), buildTestVO(2L, "李四")));

            mockMvc.perform(get("/api/basic/doctor/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].doctorName").value("张三"))
                    .andExpect(jsonPath("$.data[1].doctorName").value("李四"));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(doctorService.getById(1L)).thenReturn(buildTestVO(1L, "张三"));

            mockMvc.perform(get("/api/basic/doctor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.doctorName").value("张三"));
        }

        @Test
        @DisplayName("getById: 不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(doctorService.getById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND));

            mockMvc.perform(get("/api/basic/doctor/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(700))
                    .andExpect(jsonPath("$.message").value("医生不存在"));
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
                    "doctorName", "新医生",
                    "hospitalId", 1,
                    "hospitalDeptId", 1
            );

            mockMvc.perform(post("/api/basic/doctor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "1")
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("create: 缺少必填参数时返回400")
        void create_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "hospitalId", 1
            );

            mockMvc.perform(post("/api/basic/doctor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "1")
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
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
                    "doctorName", "更新后的医生"
            );

            mockMvc.perform(put("/api/basic/doctor/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND))
                    .when(doctorService).update(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "doctorName", "更新后的医生"
            );

            mockMvc.perform(put("/api/basic/doctor/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(700))
                    .andExpect(jsonPath("$.message").value("医生不存在"));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("delete: 删除成功返回200")
        void delete_shouldSuccess() throws Exception {
            mockMvc.perform(delete("/api/basic/doctor/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("delete: 数据不存在时返回错误")
        void delete_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND))
                    .when(doctorService).remove(999L);

            mockMvc.perform(delete("/api/basic/doctor/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(700));
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() throws Exception {
            mockMvc.perform(put("/api/basic/doctor/1/status")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("updateStatus: 非法状态值时返回400")
        void updateStatus_whenInvalidStatus_shouldReturnError() throws Exception {
            mockMvc.perform(put("/api/basic/doctor/1/status")
                            .param("status", "99"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("must be less than or equal to 1"));
        }
    }

    // ==================== suggest 测试 ====================

    @Nested
    @DisplayName("suggest 测试")
    class SuggestTests {

        @Test
        @DisplayName("suggest: 返回联想列表")
        void suggest_shouldReturnList() throws Exception {
            when(doctorService.listByCreatorAndHospital(eq(1L), eq(1L), any()))
                    .thenReturn(List.of(buildTestVO(1L, "张三")));

            mockMvc.perform(get("/api/basic/doctor/suggest")
                            .param("creatorId", "1")
                            .param("hospitalId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].doctorName").value("张三"));
        }

        @Test
        @DisplayName("suggest: 无数据时返回空列表")
        void suggest_whenEmpty_shouldReturnEmptyList() throws Exception {
            when(doctorService.listByCreatorAndHospital(eq(1L), eq(1L), any()))
                    .thenReturn(new ArrayList<>());

            mockMvc.perform(get("/api/basic/doctor/suggest")
                            .param("creatorId", "1")
                            .param("hospitalId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ==================== quick-add 测试 ====================

    @Nested
    @DisplayName("quick-add 测试")
    class QuickAddTests {

        @Test
        @DisplayName("quick-add: 不存在时创建成功")
        void quickAdd_whenNotExists_shouldCreate() throws Exception {
            DoctorVO newVo = buildTestVO(1L, "新医生");
            when(doctorService.quickAdd(any(), any())).thenReturn(newVo);

            Map<String, Object> requestBody = Map.of(
                    "doctorName", "新医生",
                    "hospitalId", 1
            );

            mockMvc.perform(post("/api/basic/doctor/quick-add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "1")
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.doctorName").value("新医生"));
        }

        @Test
        @DisplayName("quick-add: 已存在时返回现有医生")
        void quickAdd_whenExists_shouldReturnExisting() throws Exception {
            DoctorVO existingVo = buildTestVO(1L, "张三");
            when(doctorService.quickAdd(any(), any())).thenReturn(existingVo);

            Map<String, Object> requestBody = Map.of(
                    "doctorName", "张三",
                    "hospitalId", 1
            );

            mockMvc.perform(post("/api/basic/doctor/quick-add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "1")
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.doctorName").value("张三"));
        }
    }
}
