package com.yigongbao.module.system.auth.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.SystemTestApplication;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证 Controller 接口测试（基于 MockBean AuthService，专注 HTTP 层）
 *
 * @author hanjor
 * @date 2026-04-22
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

    @MockBean
    private AuthService authService;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ImageCaptchaApplication imageCaptchaApplication;

    // ==================== login 测试 ====================

    @Test
    @DisplayName("login: 参数正确时返回 token")
    void login_whenValid_shouldReturnToken() throws Exception {
        LoginVO vo = new LoginVO();
        vo.setToken("mock-token");
        when(authService.login(any())).thenReturn(vo);

        Map<String, Object> body = new HashMap<>();
        body.put("loginType", "PASSWORD");
        body.put("principal", "admin");
        body.put("credential", "123456");
        body.put("captchaKey", "test-captcha-key");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    @DisplayName("login: 缺少必填字段时返回参数错误")
    void login_whenMissingField_shouldReturnError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("loginType", "PASSWORD");
        // 缺少 principal 和 credential

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("login: 滑动验证码校验失败时返回 CAPTCHA_GRAPHIC_ERROR")
    void login_whenSliderCaptchaError_shouldReturnError() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR));

        Map<String, Object> body = new HashMap<>();
        body.put("loginType", "PASSWORD");
        body.put("principal", "admin");
        body.put("credential", "123456");
        body.put("captchaKey", "test-captcha-key");

        mockMvc.perform(post("/system/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(774));
    }

    // ==================== sendLoginCaptcha 测试 ====================

    @Test
    @DisplayName("sendLoginCaptcha: 参数正确时返回成功")
    void sendLoginCaptcha_shouldReturnSuccess() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("captchaType", "PHONE");
        body.put("target", "13800000001");

        mockMvc.perform(post("/system/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("sendLoginCaptcha: 缺少必填字段时返回参数错误")
    void sendLoginCaptcha_whenMissingField_shouldReturnError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("captchaType", "PHONE");
        // 缺少 target

        mockMvc.perform(post("/system/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== sendForgotPasswordCaptcha 测试 ====================

    @Test
    @DisplayName("sendForgotPasswordCaptcha: 参数正确时返回成功")
    void sendForgotPasswordCaptcha_shouldReturnSuccess() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("captchaType", "PHONE");
        body.put("target", "13999999999");

        mockMvc.perform(post("/system/auth/forgot-password/captcha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== resetPassword 测试 ====================

    @Test
    @DisplayName("resetPassword: 缺少必填字段时返回参数错误")
    void resetPassword_whenMissingField_shouldReturnError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("captchaType", "PHONE");
        body.put("target", "13800000001");
        // 缺少 captcha 和 newPassword

        mockMvc.perform(post("/system/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== logout / getCurrentUserInfo / changePassword 测试 ====================

    @Test
    @DisplayName("logout: 请求成功返回200")
    void logout_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/system/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("getCurrentUserInfo: 无用户信息时返回 null data")
    void getCurrentUserInfo_shouldReturnVO() throws Exception {
        when(authService.getCurrentUserInfo()).thenReturn(new LoginVO());
        mockMvc.perform(get("/system/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("changePassword: 新密码过短时返回参数错误")
    void changePassword_whenPasswordTooShort_shouldReturnError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", "123456");
        body.put("newPassword", "123"); // 过短

        mockMvc.perform(put("/system/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
