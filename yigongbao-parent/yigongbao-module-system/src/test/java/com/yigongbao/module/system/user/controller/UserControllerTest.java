package com.yigongbao.module.system.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 用户管理 Controller 接口测试
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
@DisplayName("用户管理 Controller 接口测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== list 测试 ====================

    @Test
    @DisplayName("list: 分页查询用户列表成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按用户名模糊查询")
    void list_withUsername_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按真实姓名模糊查询")
    void list_withRealName_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("realName", "管理员"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按机构ID筛选")
    void list_withOrgId_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按部门ID筛选")
    void list_withDeptId_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("deptId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按账户分类筛选")
    void list_withAccountType_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/api/system/user/list")
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
        mockMvc.perform(get("/api/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 查询存在的用户")
    void getById_whenExists_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/system/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").exists());
    }

    @Test
    @DisplayName("getById: 查询不存在的用户")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/user/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("数据不存在"));
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建用户成功")
    void create_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testuser");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000000");
        requestBody.put("email", "test@test.com");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("create: 用户名重复")
    void create_whenUsernameExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000000");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("create: 手机号重复")
    void create_whenPhoneExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13800000001");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("create: 用户名为空")
    void create_whenUsernameEmpty_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000001");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create: 密码长度不足")
    void create_whenPasswordTooShort_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser2");
        requestBody.put("password", "123");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000002");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create: 手机号格式错误")
    void create_whenPhoneInvalid_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser3");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "12345");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create: 机构不存在")
    void create_whenOrgNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser4");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000003");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 999999);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("create: 部门不存在")
    void create_whenDeptNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser5");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000004");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);
        requestBody.put("deptId", 999999);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("create: 角色不存在")
    void create_whenRoleNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser6");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000005");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);
        requestBody.put("roleId", 999999);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("create: 密码可选（不传密码）")
    void create_whenPasswordOptional_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser7");
        // 不传password字段
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000006");
        requestBody.put("accountType", 1);
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/api/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新用户成功")
    void update_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("realName", "更新后的姓名");
        requestBody.put("email", "update@test.com");

        mockMvc.perform(put("/api/system/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("update: 更新不存在的用户")
    void update_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("realName", "测试用户");

        mockMvc.perform(put("/api/system/user/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("update: 手机号与其他用户重复")
    void update_whenPhoneExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", "13800000002");

        mockMvc.perform(put("/api/system/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    // ==================== delete 测试 ====================

    @Test
    @DisplayName("delete: 删除存在的用户")
    void delete_whenExists_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/api/system/user/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("delete: 删除不存在的用户")
    void delete_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/user/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 启用用户")
    void updateStatus_enable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/user/6/status")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 禁用用户")
    void updateStatus_disable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/user/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 无效的状态值")
    void updateStatus_invalidStatus_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/user/1/status")
                        .param("status", "2"))
                .andExpect(status().isBadRequest());
    }

    // ==================== resetPassword 测试 ====================

    @Test
    @DisplayName("resetPassword: 重置密码成功")
    void resetPassword_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("newPassword", "123456");

        mockMvc.perform(put("/api/system/user/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("resetPassword: 重置不存在的用户")
    void resetPassword_whenNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("newPassword", "123456");

        mockMvc.perform(put("/api/system/user/999999/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== changePassword 测试 ====================

    @Test
    @DisplayName("changePassword: 修改密码成功")
    void changePassword_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/user/1/change-password")
                        .param("oldPassword", "123456")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("changePassword: 旧密码错误")
    void changePassword_whenOldPasswordWrong_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/user/1/change-password")
                        .param("oldPassword", "wrongpassword")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("changePassword: 不存在的用户")
    void changePassword_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/system/user/999999/change-password")
                        .param("oldPassword", "123456")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== updateProfile 测试 ====================

    @Test
    @DisplayName("updateProfile: 用户自更新成功")
    void updateProfile_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", "13900000999");
        requestBody.put("avatar", "/avatar/new.png");

        mockMvc.perform(put("/api/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateProfile: 手机号与其他用户重复")
    void updateProfile_whenPhoneExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", "13800000002");
        requestBody.put("avatar", "/avatar/new.png");

        mockMvc.perform(put("/api/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
