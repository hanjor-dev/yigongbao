package com.yigongbao.module.system.dept.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 部门管理 Controller 接口测试
 * 使用 MockMvc 进行 HTTP 接口测试
 *
 * @author hanjor
 * @date 2026-03-17
 */
@SpringBootTest(
    classes = com.yigongbao.module.system.SystemTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("部门管理 Controller 接口测试")
class DeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeptMapper deptMapper;

    // ==================== list 测试 ====================

    @Test
    @DisplayName("list: 分页查询部门列表成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/api/system/dept/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按部门名称模糊查询")
    void list_withDeptName_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/dept/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("deptName", "研发"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按机构ID筛选")
    void list_withOrgId_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/dept/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按状态筛选")
    void list_withStatus_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/dept/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 存在数据时返回部门详情")
    void getById_whenExists_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/system/dept/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.deptName").value("研发部"))
                .andExpect(jsonPath("$.data.deptCode").value("DEPT-001"));
    }

    @Test
    @DisplayName("getById: 数据不存在时返回错误")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/dept/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(613))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建成功")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deptName", "新部门");
        requestBody.put("orgId", 1);
        requestBody.put("remark", "测试备注");

        mockMvc.perform(post("/api/system/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("create: 部门名称为空时参数校验失败")
    void create_whenNameEmpty_shouldReturnValidationError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("create: 所属机构不存在时返回错误")
    void create_whenOrgNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deptName", "测试部门");
        requestBody.put("orgId", 999999L);

        mockMvc.perform(post("/api/system/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(616))
                .andExpect(jsonPath("$.message").value("所属机构不存在"));
    }

    @Test
    @DisplayName("create: 部门名称已存在时返回错误")
    void create_whenNameExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deptName", "研发部");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(614))
                .andExpect(jsonPath("$.message").value("部门名称已存在"));
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deptName", "研发部更新");
        requestBody.put("remark", "更新后的备注");

        mockMvc.perform(put("/api/system/dept/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("update: 部门不存在时返回错误")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deptName", "测试部门");

        mockMvc.perform(put("/api/system/dept/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(613))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除成功")
    void remove_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/api/system/dept/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("remove: 部门不存在时返回错误")
    void remove_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/dept/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(613))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/dept/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 部门不存在时返回错误")
    void updateStatus_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/dept/999999/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(613))
                .andExpect(jsonPath("$.message").value("部门不存在"));
    }

    @Test
    @DisplayName("updateStatus: 状态值无效时参数校验失败")
    void updateStatus_whenInvalidStatus_shouldReturnValidationError() throws Exception {
        mockMvc.perform(put("/api/system/dept/1/status")
                        .param("status", "2"))
                .andExpect(status().isBadRequest());
    }
}
