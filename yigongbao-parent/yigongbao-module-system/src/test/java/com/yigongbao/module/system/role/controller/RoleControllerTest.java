package com.yigongbao.module.system.role.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
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
 * 角色管理 Controller 接口测试
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
@DisplayName("角色管理 Controller 接口测试")
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== list 测试 ====================

    @Test
    @DisplayName("list: 分页查询角色列表成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/api/system/role/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按角色名称模糊查询")
    void list_withRoleName_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/role/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("roleName", "管理员"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按账户分类筛选")
    void list_withAccountType_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/role/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("accountType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按状态筛选")
    void list_withStatus_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/role/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 查询存在的角色")
    void getById_whenExists_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/system/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.roleName").exists());
    }

    @Test
    @DisplayName("getById: 查询不存在的角色")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/role/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("数据不存在"));
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建角色成功")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleName", "测试角色");
        requestBody.put("roleCode", "ROLE_TEST");
        requestBody.put("roleDesc", "测试角色描述");
        requestBody.put("accountType", 1);
        requestBody.put("dataScope", 2);

        mockMvc.perform(post("/api/system/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("create: 角色编码重复")
    void create_whenRoleCodeExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleName", "测试角色");
        requestBody.put("roleCode", "ROLE_ADMIN");
        requestBody.put("accountType", 1);

        mockMvc.perform(post("/api/system/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(622))
                .andExpect(jsonPath("$.message").value("角色编码已存在"));
    }

    @Test
    @DisplayName("create: 角色名称为空")
    void create_whenRoleNameEmpty_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleCode", "ROLE_TEST2");
        requestBody.put("accountType", 1);

        mockMvc.perform(post("/api/system/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create: 无效数据范围值")
    void create_whenInvalidDataScope_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleName", "测试角色");
        requestBody.put("roleCode", "ROLE_TEST3");
        requestBody.put("accountType", 1);
        requestBody.put("dataScope", 99);

        mockMvc.perform(post("/api/system/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新角色成功")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleName", "更新后的角色名");
        requestBody.put("roleDesc", "更新后的描述");

        mockMvc.perform(put("/api/system/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("update: 更新不存在的角色")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleName", "测试角色");

        mockMvc.perform(put("/api/system/role/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("update: 角色编码与其他角色重复")
    void update_whenRoleCodeExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roleCode", "ROLE_DESIGNER");

        mockMvc.perform(put("/api/system/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(622))
                .andExpect(jsonPath("$.message").value("角色编码已存在"));
    }

    // ==================== delete 测试 ====================

    @Test
    @DisplayName("delete: 删除存在的角色")
    void delete_whenExists_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/api/system/role/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("delete: 删除不存在的角色")
    void delete_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/role/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("delete: 删除有关联用户的角色")
    void delete_whenHasUsers_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(623))
                .andExpect(jsonPath("$.message").value("该角色下存在用户，无法删除"));
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 启用角色")
    void updateStatus_enable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/role/8/status")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 禁用角色")
    void updateStatus_disable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/role/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 无效的状态值")
    void updateStatus_invalidStatus_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/role/1/status")
                        .param("status", "2"))
                .andExpect(status().isBadRequest());
    }
}
