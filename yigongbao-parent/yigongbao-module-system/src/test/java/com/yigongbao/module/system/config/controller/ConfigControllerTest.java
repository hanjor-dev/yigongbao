package com.yigongbao.module.system.config.controller;

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
 * 配置 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-18
 */
@SpringBootTest(classes = SystemTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ConfigController 接口测试")
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== list 测试 ====================

    /**
     * 测试用例：分页查询配置列表 - 成功场景
     */
    @Test
    @DisplayName("list: 分页查询成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/system/config/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试用例：分页查询配置列表 - 带筛选条件
     */
    @Test
    @DisplayName("list: 带筛选条件查询成功")
    void list_withFilters_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/system/config/list")
                        .param("configKey", "default.password")
                        .param("configGroup", "security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== getById 测试 ====================

    /**
     * 测试用例：根据ID查询配置 - 成功场景
     */
    @Test
    @DisplayName("getById: 存在数据时返回配置详情")
    void getById_whenExists_shouldReturnData() throws Exception {
        mockMvc.perform(get("/system/config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.configKey").exists());
    }

    /**
     * 测试用例：根据ID查询配置 - 数据不存在
     */
    @Test
    @DisplayName("getById: 数据不存在时返回错误码")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/system/config/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(626))
                .andExpect(jsonPath("$.message").value("配置不存在"));
    }

    // ==================== create 测试 ====================

    /**
     * 测试用例：创建配置 - 成功场景
     */
    @Test
    @DisplayName("create: 创建成功返回success")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configKey", "test.config.key");
        requestBody.put("configName", "测试配置");
        requestBody.put("configValue", "testValue");
        requestBody.put("configType", "string");
        requestBody.put("configGroup", "other");
        requestBody.put("configDesc", "测试描述");
        requestBody.put("isSystem", 0);
        requestBody.put("isPublic", 1);
        requestBody.put("sort", 0);
        requestBody.put("status", 1);

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：创建配置 - 参数校验失败（缺少必填字段）
     */
    @Test
    @DisplayName("create: 参数校验失败时返回错误码")
    void create_whenValidationFailed_shouldReturnError() throws Exception {
        // 缺少必填字段 configKey
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configName", "测试配置");

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用例：创建配置 - configKey 重复
     */
    @Test
    @DisplayName("create: configKey重复时返回错误码")
    void create_whenKeyExists_shouldReturnError() throws Exception {
        // 使用已存在的 configKey
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configKey", "default.password");
        requestBody.put("configName", "测试配置");
        requestBody.put("configGroup", "security");

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(627))
                .andExpect(jsonPath("$.message").value("配置键已存在"));
    }

    // ==================== update 测试 ====================

    /**
     * 测试用例：更新配置 - 成功场景
     */
    @Test
    @DisplayName("update: 更新成功返回success")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configName", "更新后的配置名称");
        requestBody.put("configValue", "updatedValue");

        // 使用 id=2（系统配置，非系统内置）
        mockMvc.perform(put("/system/config/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试用例：更新配置 - 数据不存在
     */
    @Test
    @DisplayName("update: 数据不存在时返回错误码")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configName", "更新后的配置名称");

        mockMvc.perform(put("/system/config/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(626));
    }

    /**
     * 测试用例：更新配置 - 系统内置不可修改
     */
    @Test
    @DisplayName("update: 系统内置配置不可修改")
    void update_whenSystemConfig_shouldReturnError() throws Exception {
        // id=1 是系统内置配置
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("configName", "尝试修改系统配置");

        mockMvc.perform(put("/system/config/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(628))
                .andExpect(jsonPath("$.message").value("系统内置配置不可修改"));
    }

    // ==================== delete 测试 ====================

    /**
     * 测试用例：删除配置 - 成功场景
     */
    @Test
    @DisplayName("delete: 删除成功返回success")
    void delete_shouldSuccess() throws Exception {
        // schema.sql 中 id=5 是非系统内置配置，可以删除
        mockMvc.perform(delete("/system/config/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试用例：删除配置 - 数据不存在
     */
    @Test
    @DisplayName("delete: 数据不存在时返回错误码")
    void delete_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/system/config/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(626));
    }

    /**
     * 测试用例：删除配置 - 系统内置不可删除
     */
    @Test
    @DisplayName("delete: 系统内置配置不可删除")
    void delete_whenSystemConfig_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/system/config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(629))
                .andExpect(jsonPath("$.message").value("系统内置配置不可删除"));
    }

    // ==================== listPublic 测试 ====================

    /**
     * 测试用例：获取公开配置列表 - 成功场景
     */
    @Test
    @DisplayName("listPublic: 获取公开配置成功")
    void listPublic_shouldReturnPublicConfigs() throws Exception {
        mockMvc.perform(get("/system/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
