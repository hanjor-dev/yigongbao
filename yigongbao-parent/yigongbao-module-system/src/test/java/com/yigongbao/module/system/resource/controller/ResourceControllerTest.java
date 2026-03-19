package com.yigongbao.module.system.resource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.SystemTestApplication;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 资源管理 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-19
 */
@SpringBootTest(classes = SystemTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ResourceController 接口测试")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== getResourceTree 测试 ====================

    /**
     * 测试用例：获取资源树 - 成功场景
     */
    @Test
    @DisplayName("getResourceTree: 返回资源树结构")
    void getResourceTree_shouldReturnTreeStructure() throws Exception {
        mockMvc.perform(get("/api/system/resource/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== list 测试 ====================

    /**
     * 测试用例：分页查询资源列表 - 成功场景
     */
    @Test
    @DisplayName("list: 分页查询成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/api/system/resource/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试用例：分页查询资源列表 - 带筛选条件
     */
    @Test
    @DisplayName("list: 带筛选条件查询成功")
    void list_withFilters_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/resource/list")
                        .param("resourceType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== getById 测试 ====================

    /**
     * 测试用例：根据ID查询资源 - 成功场景
     */
    @Test
    @DisplayName("getById: 存在数据时返回资源详情")
    void getById_whenExists_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/system/resource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.resourceName").exists());
    }

    /**
     * 测试用例：根据ID查询资源 - 数据不存在
     */
    @Test
    @DisplayName("getById: 数据不存在时返回错误码")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/resource/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(630))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    // ==================== create 测试 ====================

    /**
     * 测试用例：创建资源 - 成功场景
     */
    @Test
    @DisplayName("create: 创建成功返回success")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parentId", 0);
        requestBody.put("resourceName", "测试资源");
        requestBody.put("resourceCode", "test:controller");
        requestBody.put("resourceType", 1);
        requestBody.put("sort", 999);
        requestBody.put("visible", 1);
        requestBody.put("status", 1);

        mockMvc.perform(post("/api/system/resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：创建资源 - 资源编码重复
     */
    @Test
    @DisplayName("create: 资源编码重复时返回错误码")
    void create_whenCodeDuplicate_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parentId", 0);
        requestBody.put("resourceName", "测试资源");
        requestBody.put("resourceCode", "system"); // 已存在的编码
        requestBody.put("resourceType", 1);

        mockMvc.perform(post("/api/system/resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(631))
                .andExpect(jsonPath("$.message").value("资源编码已存在"));
    }

    /**
     * 测试用例：创建资源 - 参数校验失败（缺少必填字段）
     */
    @Test
    @DisplayName("create: 缺少必填字段时返回参数错误")
    void create_whenValidationFailed_shouldReturnError() throws Exception {
        // 缺少必填字段 resourceCode
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parentId", 0);
        requestBody.put("resourceName", "测试资源");
        // 缺少 resourceCode

        mockMvc.perform(post("/api/system/resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== update 测试 ====================

    /**
     * 测试用例：更新资源 - 成功场景
     */
    @Test
    @DisplayName("update: 更新成功返回success")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parentId", 0);
        requestBody.put("resourceName", "系统管理（已修改）");
        requestBody.put("resourceCode", "system");
        requestBody.put("resourceType", 1);
        requestBody.put("sort", 100);
        requestBody.put("visible", 1);
        requestBody.put("status", 1);

        mockMvc.perform(put("/api/system/resource/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：更新资源 - 数据不存在
     */
    @Test
    @DisplayName("update: 数据不存在时返回错误码")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parentId", 0);
        requestBody.put("resourceName", "测试");
        requestBody.put("resourceCode", "test");
        requestBody.put("resourceType", 1);

        mockMvc.perform(put("/api/system/resource/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(630));
    }

    // ==================== delete 测试 ====================

    /**
     * 测试用例：删除资源 - 存在子资源时失败
     */
    @Test
    @DisplayName("delete: 存在子资源时返回错误码")
    void delete_whenHasChildren_shouldReturnError() throws Exception {
        // system 资源下有子资源
        mockMvc.perform(delete("/api/system/resource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(632))
                .andExpect(jsonPath("$.message").value("该资源下存在子资源，无法删除"));
    }

    /**
     * 测试用例：删除资源 - 数据不存在
     */
    @Test
    @DisplayName("delete: 数据不存在时返回错误码")
    void delete_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/resource/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(630));
    }

    // ==================== getRoleResources 测试 ====================

    /**
     * 测试用例：获取角色资源 - 成功场景
     */
    @Test
    @DisplayName("getRoleResources: 返回角色关联的资源ID列表")
    void getRoleResources_shouldReturnResourceIds() throws Exception {
        // 超级管理员角色关联所有资源
        mockMvc.perform(get("/api/system/resource/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 测试用例：获取角色资源 - 无资源关联时返回空数组
     */
    @Test
    @DisplayName("getRoleResources: 无资源关联时返回空数组")
    void getRoleResources_whenNoResources_shouldReturnEmptyArray() throws Exception {
        // 根据 schema.sql，roleId=99 不存在，关联表无数据
        mockMvc.perform(get("/api/system/resource/role/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== assignRoleResources 测试 ====================

    /**
     * 测试用例：分配角色资源 - 成功场景
     */
    @Test
    @DisplayName("assignRoleResources: 分配成功返回success")
    void assignRoleResources_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/resource/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[101, 102]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：分配角色资源 - 清空资源
     */
    @Test
    @DisplayName("assignRoleResources: 清空资源成功")
    void assignRoleResources_whenEmpty_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/resource/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
