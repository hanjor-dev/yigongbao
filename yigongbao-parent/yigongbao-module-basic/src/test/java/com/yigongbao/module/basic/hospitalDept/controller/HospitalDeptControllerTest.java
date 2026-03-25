package com.yigongbao.module.basic.hospitalDept.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
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
@DisplayName("HospitalDeptController 接口测试")
class HospitalDeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HospitalDeptService hospitalDeptService;

    private HospitalDeptVO buildTestVO(Long id, String name) {
        HospitalDeptVO vo = new HospitalDeptVO();
        vo.setId(id);
        vo.setHospitalDeptCode("HDEPT-" + String.format("%04d", id));
        vo.setHospitalDeptName(name);
        vo.setSort(1);
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
            HospitalDeptVO vo = buildTestVO(1L, "骨科");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<HospitalDeptVO>(1, 10);
            page.setRecords(List.of(vo));
            page.setTotal(1);
            when(hospitalDeptService.listDepts(eq(1), eq(10), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/hospital-dept/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.records[0].hospitalDeptName").value("骨科"));
        }

        @Test
        @DisplayName("page: 空数据时返回空分页")
        void page_whenEmpty_shouldReturnEmptyPage() throws Exception {
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<HospitalDeptVO>(1, 10);
            page.setRecords(new ArrayList<>());
            page.setTotal(0);
            when(hospitalDeptService.listDepts(eq(1), eq(10), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/hospital-dept/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records").isEmpty());
        }

        @Test
        @DisplayName("page: 按名称筛选")
        void page_withName_shouldFilter() throws Exception {
            HospitalDeptVO vo = buildTestVO(1L, "骨科");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<HospitalDeptVO>(1, 10);
            page.setRecords(List.of(vo));
            page.setTotal(1);
            when(hospitalDeptService.listDepts(eq(1), eq(10), eq("骨科"), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/hospital-dept/page")
                            .param("hospitalDeptName", "骨科"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records[0].hospitalDeptName").value("骨科"));
        }

        @Test
        @DisplayName("page: 按状态筛选")
        void page_withStatus_shouldFilter() throws Exception {
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<HospitalDeptVO>(1, 10);
            page.setRecords(new ArrayList<>());
            page.setTotal(0);
            when(hospitalDeptService.listDepts(eq(1), eq(10), any(), eq(0)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/hospital-dept/page")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== list 测试 ====================

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("list: 返回所有科室")
        void list_shouldReturnAll() throws Exception {
            when(hospitalDeptService.listAll(any(), any()))
                    .thenReturn(List.of(buildTestVO(1L, "骨科"), buildTestVO(2L, "口腔科")));

            mockMvc.perform(get("/api/basic/hospital-dept/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].hospitalDeptName").value("骨科"))
                    .andExpect(jsonPath("$.data[1].hospitalDeptName").value("口腔科"));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(hospitalDeptService.getById(1L)).thenReturn(buildTestVO(1L, "骨科"));

            mockMvc.perform(get("/api/basic/hospital-dept/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.hospitalDeptName").value("骨科"));
        }

        @Test
        @DisplayName("getById: 不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(hospitalDeptService.getById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND));

            mockMvc.perform(get("/api/basic/hospital-dept/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(690))
                    .andExpect(jsonPath("$.message").value("科室不存在"));
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
                    "hospitalDeptName", "测试科室",
                    "sort", 1
            );

            mockMvc.perform(post("/api/basic/hospital-dept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("create: 科室名称已存在时返回错误")
        void create_whenNameExists_shouldReturnError() throws Exception {
            // 模拟 Service 层抛出科室已存在异常
            doThrow(new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_EXISTS))
                    .when(hospitalDeptService).create(any());

            Map<String, Object> requestBody = Map.of(
                    "hospitalDeptCode", "HDEPT-NEW",
                    "hospitalDeptName", "骨科"
            );

            mockMvc.perform(post("/api/basic/hospital-dept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(691));
        }

        @Test
        @DisplayName("create: 缺少必填参数时返回400")
        void create_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "sort", 1
            );

            mockMvc.perform(post("/api/basic/hospital-dept")
                            .contentType(MediaType.APPLICATION_JSON)
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
                    "hospitalDeptName", "神经外科"
            );

            mockMvc.perform(put("/api/basic/hospital-dept/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND))
                    .when(hospitalDeptService).update(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "hospitalDeptName", "神经外科"
            );

            mockMvc.perform(put("/api/basic/hospital-dept/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(690))
                    .andExpect(jsonPath("$.message").value("科室不存在"));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("delete: 删除成功返回200")
        void delete_shouldSuccess() throws Exception {
            mockMvc.perform(delete("/api/basic/hospital-dept/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("delete: 数据不存在时返回错误")
        void delete_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND))
                    .when(hospitalDeptService).remove(999L);

            mockMvc.perform(delete("/api/basic/hospital-dept/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(690));
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() throws Exception {
            mockMvc.perform(put("/api/basic/hospital-dept/1/status")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("updateStatus: 非法状态值时返回400")
        void updateStatus_whenInvalidStatus_shouldReturnError() throws Exception {
            mockMvc.perform(put("/api/basic/hospital-dept/1/status")
                            .param("status", "99"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("must be less than or equal to 1"));
        }
    }
}
