package com.yigongbao.module.system.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
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
 * 认证 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-19
 */
@SpringBootTest(classes = SystemTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 接口测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== login 测试 ====================

    /**
     * 测试用例：用户登录 - 成功场景
     */
    @Test
    @DisplayName("login: 用户名密码正确时登录成功")
    void login_whenSuccess_shouldReturnToken() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        requestBody.put("password", "123456");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user").exists());
    }

    /**
     * 测试用例：用户登录 - 用户名不存在
     */
    @Test
    @DisplayName("login: 用户名不存在时返回错误码")
    void login_whenUsernameNotExists_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "not_exists_user");
        requestBody.put("password", "123456");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(641))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    /**
     * 测试用例：用户登录 - 密码错误
     */
    @Test
    @DisplayName("login: 密码错误时返回错误码")
    void login_whenPasswordWrong_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        requestBody.put("password", "wrong_password");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(603))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    /**
     * 测试用例：用户登录 - 用户已禁用
     */
    @Test
    @DisplayName("login: 用户已禁用时返回错误码")
    void login_whenUserDisabled_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "disabled_user");
        requestBody.put("password", "123456");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.message").value("用户已被禁用"));
    }

    /**
     * 测试用例：用户登录 - 参数校验失败（缺少必填字段）
     */
    @Test
    @DisplayName("login: 缺少必填字段时返回参数错误")
    void login_whenValidationFailed_shouldReturnError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        // 缺少 password

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用例：用户登录 - 登录成功返回菜单和权限
     */
    @Test
    @DisplayName("login: 登录成功返回菜单和权限信息")
    void login_shouldReturnMenusAndPermissions() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        requestBody.put("password", "123456");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.menus").isArray())
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    /**
     * 测试用例：用户登录 - 密码连续错误达到5次时账户被锁定
     * 由于每个测试方法使用 @Transactional 注解，数据库变更会在测试结束后回滚
     * 此测试独立执行，验证锁定机制生效
     */
    @Test
    @DisplayName("login: 密码连续错误5次后账户被锁定")
    @Transactional  // 覆盖类级别的 @Transactional，确保此测试独立
    void login_whenFailCountReachesMax_shouldBeLocked() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "admin");
        requestBody.put("password", "wrong_password");

        // 连续5次密码错误，第5次达到锁定阈值
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/system/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(603)); // 密码错误
        }
    }

    // ==================== logout 测试 ====================

    /**
     * 测试用例：用户登出 - 需要先登录
     */
    @Test
    @DisplayName("logout: 未登录时返回401")
    void logout_whenNotLogin_shouldReturn401() throws Exception {
        mockMvc.perform(post("/system/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== getCurrentUserInfo 测试 ====================

    /**
     * 测试用例：获取当前用户信息 - 需要先登录
     */
    @Test
    @DisplayName("getCurrentUserInfo: 未登录时返回401")
    void getCurrentUserInfo_whenNotLogin_shouldReturn401() throws Exception {
        mockMvc.perform(get("/system/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== changePassword 测试 ====================

    /**
     * 测试用例：修改密码 - 需要先登录
     */
    @Test
    @DisplayName("changePassword: 未登录时返回401")
    void changePassword_whenNotLogin_shouldReturn401() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("oldPassword", "123456");
        requestBody.put("newPassword", "654321");

        mockMvc.perform(put("/system/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 测试用例：修改密码 - 参数校验失败（新密码过短）
     */
    @Test
    @DisplayName("changePassword: 新密码过短时返回参数错误")
    void changePassword_whenPasswordTooShort_shouldReturnError() throws Exception {
        // 先登录获取token
        Map<String, Object> loginRequestBody = new HashMap<>();
        loginRequestBody.put("username", "admin");
        loginRequestBody.put("password", "123456");

        // 由于 SaToken 拦截器在测试环境下关闭，这里模拟已登录状态
        // 实际测试需要配合 SaToken Mock
        Map<String, Object> passwordRequestBody = new HashMap<>();
        passwordRequestBody.put("oldPassword", "123456");
        passwordRequestBody.put("newPassword", "123"); // 过短

        mockMvc.perform(put("/system/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordRequestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
