package com.yigongbao.module.system.org.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
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
 * 机构管理 Controller 接口测试
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
@DisplayName("机构管理 Controller 接口测试")
class OrgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgMapper orgMapper;

    // ==================== list 测试 ====================

    @Test
    @DisplayName("list: 分页查询机构列表成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/api/system/org/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按机构名称模糊查询")
    void list_withOrgName_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/org/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("orgName", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按状态筛选")
    void list_withStatus_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/org/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 查询机构详情成功")
    void getById_shouldReturnOrg() throws Exception {
        mockMvc.perform(get("/api/system/org/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orgName").exists());
    }

    @Test
    @DisplayName("getById: 机构不存在时返回错误码")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/org/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(608))
                .andExpect(jsonPath("$.message").value("机构不存在"));
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建机构成功")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "接口测试机构");
        requestBody.put("orgType", 1);
        requestBody.put("contact", "测试联系人");
        requestBody.put("phone", "13900000001");
        requestBody.put("areaName", "北京市");
        requestBody.put("address", "朝阳区测试路1号");

        mockMvc.perform(post("/api/system/org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        // 验证数据库写入
        OrgEntity created = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgName, "接口测试机构"));
        assertNotNull(created);
    }

    @Test
    @DisplayName("create: 机构名称为空时参数校验失败")
    void create_whenNameEmpty_shouldReturnValidationError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "");
        requestBody.put("orgType", 1);
        requestBody.put("contact", "测试联系人");
        requestBody.put("phone", "13900000001");

        mockMvc.perform(post("/api/system/org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("create: 机构类型为空时参数校验失败")
    void create_whenTypeEmpty_shouldReturnValidationError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "测试机构");
        requestBody.put("orgType", null);
        requestBody.put("contact", "测试联系人");
        requestBody.put("phone", "13900000001");

        mockMvc.perform(post("/api/system/org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("create: 手机号格式错误时参数校验失败")
    void create_whenPhoneInvalid_shouldReturnValidationError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "测试机构");
        requestBody.put("orgType", 1);
        requestBody.put("contact", "测试联系人");
        requestBody.put("phone", "12345678901");

        mockMvc.perform(post("/api/system/org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("create: 机构名称已存在时返回业务错误")
    void create_whenNameExists_shouldReturnBusinessError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "测试医疗机构");
        requestBody.put("orgType", 1);
        requestBody.put("contact", "测试联系人");
        requestBody.put("phone", "13900000001");

        mockMvc.perform(post("/api/system/org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(609))
                .andExpect(jsonPath("$.message").value("机构名称已存在"));
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新机构成功")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "更新后的机构名称");
        requestBody.put("contact", "新联系人");
        requestBody.put("phone", "13900000002");

        mockMvc.perform(put("/api/system/org/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        // 验证数据库更新
        OrgEntity updated = orgMapper.selectById(1L);
        assertNotNull(updated);
        assertEquals("更新后的机构名称", updated.getOrgName());
    }

    @Test
    @DisplayName("update: 机构不存在时返回错误码")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orgName", "不存在的机构");

        mockMvc.perform(put("/api/system/org/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(608))
                .andExpect(jsonPath("$.message").value("机构不存在"));
    }

    @Test
    @DisplayName("update: 机构名称与其他机构重复时返回业务错误")
    void update_whenNameDuplicate_shouldReturnBusinessError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        // 将 id=1 的机构名称改为 id=2 的机构名称，造成重复
        requestBody.put("orgName", "测试生产企业");

        mockMvc.perform(put("/api/system/org/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(609))
                .andExpect(jsonPath("$.message").value("机构名称已存在"));
    }

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除机构成功")
    void remove_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/api/system/org/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        // 验证逻辑删除
        OrgEntity deleted = orgMapper.selectById(3L);
        assertNull(deleted);
    }

    @Test
    @DisplayName("remove: 机构不存在时返回错误码")
    void remove_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/org/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(608))
                .andExpect(jsonPath("$.message").value("机构不存在"));
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/org/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        // 验证状态更新
        OrgEntity updated = orgMapper.selectById(1L);
        assertNotNull(updated);
        assertEquals(0, updated.getStatus());
    }

    @Test
    @DisplayName("updateStatus: 机构不存在时返回错误码")
    void updateStatus_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/org/999999/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(608))
                .andExpect(jsonPath("$.message").value("机构不存在"));
    }

    @Test
    @DisplayName("updateStatus: 状态值超出范围时参数校验失败")
    void updateStatus_whenInvalidStatus_shouldReturnValidationError() throws Exception {
        mockMvc.perform(put("/api/system/org/1/status")
                        .param("status", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("updateStatus: 状态值为负数时参数校验失败")
    void updateStatus_whenNegativeStatus_shouldReturnValidationError() throws Exception {
        mockMvc.perform(put("/api/system/org/1/status")
                        .param("status", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
