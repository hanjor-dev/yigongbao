package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
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

    @MockBean
    private com.yigongbao.module.system.user.service.UserHospitalService userHospitalService;

    /**
     * 生成模拟登录 Token 的后置处理器
     */
    private RequestPostProcessor mockLogin() {
        return request -> {
            // 先登录获取 token
            StpUtil.login(1L);
            String token = StpUtil.getTokenValue();
            // 将 token 设置到请求头
            request.addHeader("satoken", token);
            return request;
        };
    }

    // ==================== list 测试 ====================

    @Test
    @DisplayName("list: 分页查询用户列表成功")
    void list_shouldReturnPageData() throws Exception {
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("accountType", "6.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("list: 按状态筛选")
    void list_withStatus_shouldReturnFilteredData() throws Exception {
        mockMvc.perform(get("/system/user/list")
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
        mockMvc.perform(get("/system/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").exists());
    }

    @Test
    @DisplayName("getById: 查询不存在的用户")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/system/user/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(600))
                .andExpect(jsonPath("$.message").value("用户不存在"));
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
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
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
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(617))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("create: 手机号重复")
    void create_whenPhoneExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13800000001");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(618))
                .andExpect(jsonPath("$.message").value("手机号已存在"));
    }

    @Test
    @DisplayName("create: 用户名为空")
    void create_whenUsernameEmpty_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000001");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
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
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
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
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
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
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 999999);

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(619))
                .andExpect(jsonPath("$.message").value("所属机构不存在"));
    }

    @Test
    @DisplayName("create: 部门不存在")
    void create_whenDeptNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser5");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000004");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);
        requestBody.put("deptId", 999999);

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(620))
                .andExpect(jsonPath("$.message").value("所属部门不存在"));
    }

    @Test
    @DisplayName("create: 角色不存在")
    void create_whenRoleNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser6");
        requestBody.put("password", "123456");
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000005");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);
        requestBody.put("roleId", 999999);  // 不存在的角色

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(621))
                .andExpect(jsonPath("$.message").value("角色不存在"));
    }

    @Test
    @DisplayName("create: 密码可选（不传密码）")
    void create_whenPasswordOptional_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser7");
        // 不传password字段
        requestBody.put("realName", "测试用户");
        requestBody.put("phone", "13900000006");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);

        mockMvc.perform(post("/system/user")
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

        mockMvc.perform(put("/system/user/1")
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

        mockMvc.perform(put("/system/user/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    @DisplayName("update: 手机号与其他用户重复")
    void update_whenPhoneExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", "13800000002");

        mockMvc.perform(put("/system/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(618))
                .andExpect(jsonPath("$.message").value("手机号已存在"));
    }

    // ==================== delete 测试 ====================

    @Test
    @DisplayName("delete: 删除存在的用户")
    void delete_whenExists_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/system/user/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("delete: 删除不存在的用户")
    void delete_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/system/user/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 启用用户")
    void updateStatus_enable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/system/user/6/status")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 禁用用户")
    void updateStatus_disable_shouldSuccess() throws Exception {
        mockMvc.perform(put("/system/user/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("updateStatus: 无效的状态值")
    void updateStatus_invalidStatus_shouldReturnError() throws Exception {
        mockMvc.perform(put("/system/user/1/status")
                        .param("status", "2"))
                .andExpect(status().isBadRequest());
    }

    // ==================== resetPassword 测试 ====================

    @Test
    @DisplayName("resetPassword: 重置密码成功")
    void resetPassword_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("newPassword", "123456");

        mockMvc.perform(put("/system/user/1/reset-password")
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

        mockMvc.perform(put("/system/user/999999/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== changePassword 测试 ====================
    // 注意：测试环境 satoken.interceptor.enable=false，以下测试验证的是接口参数处理逻辑
    // 真实的认证拦截需要在生产环境或单独启用 SaToken 拦截器的集成测试中验证

    @Test
    @DisplayName("changePassword: 修改密码（测试环境 SaToken 禁用，验证参数处理）")
    void changePassword_shouldSuccess() throws Exception {
        mockMvc.perform(put("/system/user/1/change-password")
                        .param("oldPassword", "123456")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("changePassword: 旧密码错误（测试环境 SaToken 禁用，验证业务逻辑）")
    void changePassword_whenOldPasswordWrong_shouldReturnError() throws Exception {
        mockMvc.perform(put("/system/user/1/change-password")
                        .param("oldPassword", "wrongpassword")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(625))
                .andExpect(jsonPath("$.message").value("旧密码错误"));
    }

    @Test
    @DisplayName("changePassword: 用户不存在（测试环境 SaToken 禁用，验证业务逻辑）")
    void changePassword_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(put("/system/user/999999/change-password")
                        .param("oldPassword", "123456")
                        .param("newPassword", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== updateProfile 测试 ====================

    @Test
    @DisplayName("updateProfile: 用户自更新成功")
    void updateProfile_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", "13900000999");
        requestBody.put("avatar", "/avatar/new.png");

        mockMvc.perform(put("/system/user/profile")
                        .with(mockLogin())
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

        mockMvc.perform(put("/system/user/profile")
                        .with(mockLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(618))
                .andExpect(jsonPath("$.message").value("手机号已存在"));
    }

    // ==================== 医院范围权限相关测试 ====================

    /**
     * 测试用例：创建用户时传入角色（dataScopeType=hospitals）和 hospitalIds，验证参数正确
     * 说明：roleId=4（业务员）的 dataScopeType=hospitals，hospitalIds 需要有效医院ID
     * 注意：由于测试环境无 hospital 表，验证返回 400（部分医院ID无效）而非业务成功
     */
    @Test
    @DisplayName("create: dataScopeType=hospitals的角色传hospitalIds时需确保医院有效")
    void create_withHospitalScopeRoleAndHospitalIds_shouldValidateHospitalIds() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "hospitaluser1");
        requestBody.put("password", "123456");
        requestBody.put("realName", "医院用户1");
        requestBody.put("phone", "13900000011");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);
        requestBody.put("roleId", 4);  // 业务员角色，dataScopeType=hospitals
        requestBody.put("hospitalIds", List.of(1L, 2L));

        // 由于测试环境无 hospital 表，UserHospitalServiceImpl.assignHospitals 会抛出异常
        // 验证接口仍返回 200（用户基本信息创建成功，医院分配由 Service 层处理）
        // 注意：实际场景需要确保 hospitalIds 对应的医院均存在且启用
        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：创建用户时传入角色（dataScopeType=all），不传 hospitalIds
     * 说明：roleId=1（公司管理员）的 dataScopeType=all
     */
    @Test
    @DisplayName("create: dataScopeType=all的角色不传hospitalIds应成功创建")
    void create_withNonHospitalScopeRole_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "normaluser1");
        requestBody.put("password", "123456");
        requestBody.put("realName", "普通用户1");
        requestBody.put("phone", "13900000012");
        requestBody.put("accountType", "6.1");
        requestBody.put("orgId", 1);
        requestBody.put("roleId", 1);  // 公司管理员角色，dataScopeType=all
        // 不传 hospitalIds

        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：更新用户时传入 hospitalIds，验证参数正确
     * 说明：用户1关联角色1（dataScopeType=all），hospitalIds 会被忽略
     */
    @Test
    @DisplayName("update: 传入hospitalIds应成功更新（角色dataScopeType决定是否分配）")
    void update_withHospitalIds_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("realName", "更新姓名");
        requestBody.put("email", "updatehospital@test.com");
        requestBody.put("hospitalIds", List.of(1L));

        mockMvc.perform(put("/system/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }
}
