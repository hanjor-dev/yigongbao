package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.ForgotPasswordResetDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.dto.SendCaptchaDTO;
import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import com.yigongbao.module.system.auth.mapper.LoginLogMapper;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 认证 Service 单元测试
 * 使用 Mockito 进行单元测试，不依赖真实数据库
 *
 * @author hanjor
 * @date 2026-04-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 单元测试")
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginLogMapper loginLogMapper;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ConfigService configService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private ImageCaptchaApplication imageCaptchaApplication;

    @InjectMocks
    private AuthServiceImpl authService;

    private MockedStatic<StpUtil> stpUtilMockedStatic;

    private UserEntity testUser;
    /** 不带滑动验证码的 PASSWORD 登录 DTO（兼容过渡期） */
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // Mock Sa-Token 静态方法
        stpUtilMockedStatic = mockStatic(StpUtil.class);

        // Mock StpUtil.getSession()
        cn.dev33.satoken.session.SaSession mockSession = mock(cn.dev33.satoken.session.SaSession.class);
        stpUtilMockedStatic.when(StpUtil::getSession).thenReturn(mockSession);

        // 初始化测试用户
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"); // 123456 的 BCrypt 加密
        testUser.setRealName("系统管理员");
        testUser.setPhone("13800000001");
        testUser.setEmail("admin@example.com");
        testUser.setStatus(1);
        testUser.setRoleId(1L);
        testUser.setRoleName("超级管理员");

        // 不带验证码的基础 PASSWORD 登录 DTO（过渡期跳过验证码校验）
        loginDTO = new LoginDTO();
        loginDTO.setLoginType(LoginTypeEnum.PASSWORD);
        loginDTO.setPrincipal("admin");
        loginDTO.setCredential("123456");
        // captchaKey 和 captchaTrack 均不设置，过渡期跳过滑动验证码校验
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMockedStatic != null) {
            stpUtilMockedStatic.close();
        }
    }

    // ==================== login - PASSWORD 基础流程 ====================

    @Test
    @DisplayName("login(PASSWORD): 用户名密码正确时登录成功")
    void login_whenSuccess_shouldReturnToken() {
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(loginDTO);

        assertNotNull(result);
        assertEquals("mock-token", result.getToken());
        verify(userMapper).selectByUsername("admin");
        verify(passwordEncoder).matches("123456", testUser.getPassword());
        verify(loginLogMapper).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login(PASSWORD): 用户名不存在时抛出 USERNAME_OR_PASSWORD_ERROR")
    void login_whenUsernameNotExists_shouldThrowException() {
        loginDTO.setPrincipal("not_exists_user");
        when(userMapper.selectByUsername("not_exists_user")).thenReturn(null);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
        verify(loginLogMapper).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login(PASSWORD): 用户已禁用时抛出 USER_DISABLED")
    void login_whenUserDisabled_shouldThrowException() {
        testUser.setStatus(StatusConstants.DISABLED);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.USER_DISABLED.getCode(), ex.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login(PASSWORD): 密码错误时抛出 PASSWORD_ERROR")
    void login_whenPasswordWrong_shouldThrowException() {
        loginDTO.setCredential("wrong_password");
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("login(PASSWORD): 账户已锁定时拒绝登录")
    void login_whenAccountLocked_shouldThrowException() {
        testUser.setLoginFailCount(5);
        testUser.setLockTime(LocalDateTime.now()); // 刚刚锁定
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.ACCOUNT_LOCKED.getCode(), ex.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login(PASSWORD): 账户锁定超时后自动解锁可正常登录")
    void login_whenLockExpired_shouldAllowLogin() {
        testUser.setLoginFailCount(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(30)); // 已超时（锁定15分钟）
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(loginDTO);

        assertNotNull(result.getToken());
    }

    @Test
    @DisplayName("login(PASSWORD): 密码错误时递增失败计数但未达阈值不锁定")
    void login_whenPasswordWrong_shouldIncrementFailCount() {
        testUser.setLoginFailCount(3);
        loginDTO.setCredential("wrong_password");
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> authService.login(loginDTO));

        // 失败计数 3 → 4，未达阈值 5，不应设置 lockTime
        verify(userMapper).updateById(argThat((UserEntity u) ->
                u.getLoginFailCount() == 4 && u.getLockTime() == null
        ));
    }

    @Test
    @DisplayName("login(PASSWORD): 密码错误达阈值时锁定账户")
    void login_whenFailCountReachesMax_shouldLockAccount() {
        testUser.setLoginFailCount(4); // 再错一次即达到阈值 5
        loginDTO.setCredential("wrong_password");
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> authService.login(loginDTO));

        // 失败计数 4 → 5，应设置 lockTime
        verify(userMapper).updateById(argThat((UserEntity u) ->
                u.getLoginFailCount() == 5 && u.getLockTime() != null
        ));
    }

    @Test
    @DisplayName("login(PASSWORD): 登录成功后重置失败计数")
    void login_whenSuccess_shouldResetFailCount() {
        testUser.setLoginFailCount(3);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        authService.login(loginDTO);

        verify(userMapper).updateById(argThat((UserEntity u) ->
                u.getLoginFailCount() == 0 && u.getLockTime() == null
        ));
    }

    // ==================== login - 滑动验证码 ====================

    @Test
    @DisplayName("login(PASSWORD): 携带有效滑动验证码时校验通过后登录成功")
    void login_withValidSliderCaptcha_shouldLoginSuccess() {
        // 构造带验证码的 DTO
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PASSWORD);
        dto.setPrincipal("admin");
        dto.setCredential("123456");
        dto.setCaptchaKey("captcha-key-001");
        dto.setCaptchaTrack(new ImageCaptchaTrack());

        // 滑动验证码校验通过
        when(imageCaptchaApplication.matching(eq("captcha-key-001"), any(ImageCaptchaTrack.class)))
                .thenReturn(ApiResponse.ofSuccess());
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(dto);

        assertNotNull(result.getToken());
        verify(imageCaptchaApplication).matching(eq("captcha-key-001"), any(ImageCaptchaTrack.class));
    }

    @Test
    @DisplayName("login(PASSWORD): 滑动验证码校验失败时抛出 CAPTCHA_GRAPHIC_ERROR")
    void login_whenSliderCaptchaFailed_shouldThrowError() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PASSWORD);
        dto.setPrincipal("admin");
        dto.setCredential("123456");
        dto.setCaptchaKey("captcha-key-001");
        dto.setCaptchaTrack(new ImageCaptchaTrack());

        // 滑动验证码校验失败
        when(imageCaptchaApplication.matching(eq("captcha-key-001"), any(ImageCaptchaTrack.class)))
                .thenReturn(ApiResponse.ofError("验证失败"));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR.getCode(), ex.getCode());
        // 验证码失败时不应继续查询用户
        verify(userMapper, never()).selectByUsername(any());
    }

    @Test
    @DisplayName("login(PASSWORD): 不传验证码时跳过滑动验证码校验（过渡期兼容）")
    void login_withoutCaptcha_shouldSkipSliderVerify() {
        // loginDTO 未设置 captchaKey 和 captchaTrack
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(loginDTO);

        assertNotNull(result.getToken());
        // 不应调用 imageCaptchaApplication
        verify(imageCaptchaApplication, never()).matching(any(), any(ImageCaptchaTrack.class));
    }

    // ==================== login - PHONE ====================

    @Test
    @DisplayName("login(PHONE): 手机验证码正确时登录成功")
    void login_byPhone_whenSuccess_shouldReturnToken() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PHONE);
        dto.setPrincipal("13800000001");
        dto.setCredential("123456");

        doNothing().when(captchaService).verifyCaptcha(anyString(), eq("13800000001"), anyString(), anyString());
        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(dto);

        assertNotNull(result.getToken());
        verify(captchaService).verifyCaptcha(anyString(), eq("13800000001"), anyString(), anyString());
    }

    @Test
    @DisplayName("login(PHONE): 手机号未注册时抛出 USER_NOT_FOUND")
    void login_byPhone_whenUserNotFound_shouldThrow() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PHONE);
        dto.setPrincipal("13900000000");
        dto.setCredential("123456");

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByPhone("13900000000")).thenReturn(null);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("login(PHONE): 手机号对应用户已禁用时抛出 USER_DISABLED")
    void login_byPhone_whenUserDisabled_shouldThrow() {
        testUser.setStatus(StatusConstants.DISABLED);
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PHONE);
        dto.setPrincipal("13800000001");
        dto.setCredential("123456");

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ErrorCodeEnum.USER_DISABLED.getCode(), ex.getCode());
    }

    // ==================== login - PASSWORD by email ====================

    @Test
    @DisplayName("login(PASSWORD): 用邮箱作为账号时自动匹配邮箱字段登录成功")
    void login_byEmail_asPassword_whenSuccess_shouldReturnToken() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PASSWORD);
        dto.setPrincipal("admin@example.com");
        dto.setCredential("123456");

        when(userMapper.selectByEmail("admin@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(dto);

        assertNotNull(result.getToken());
        // 应走邮箱查询路径，不调用 selectByUsername
        verify(userMapper).selectByEmail("admin@example.com");
        verify(userMapper, never()).selectByUsername(any());
    }

    @Test
    @DisplayName("login(PASSWORD): 用户名方式不走邮箱查询路径")
    void login_byUsername_shouldNotQueryByEmail() {
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        authService.login(loginDTO);

        verify(userMapper).selectByUsername("admin");
        verify(userMapper, never()).selectByEmail(any());
    }

    @Test
    @DisplayName("login(PASSWORD): 邮箱格式但未注册时抛出 USERNAME_OR_PASSWORD_ERROR")
    void login_byEmail_asPassword_whenNotFound_shouldThrow() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.PASSWORD);
        dto.setPrincipal("notexist@example.com");
        dto.setCredential("123456");

        when(userMapper.selectByEmail("notexist@example.com")).thenReturn(null);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
    }

    // ==================== logout ====================

    @Test
    @DisplayName("logout: 已登录时登出成功")
    void logout_whenLoggedIn_shouldSuccess() {
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        assertDoesNotThrow(() -> authService.logout());

        stpUtilMockedStatic.verify(StpUtil::logout);
    }

    // ==================== getCurrentUserInfo ====================

    @Test
    @DisplayName("getCurrentUserInfo: 用户存在时返回完整 VO")
    void getCurrentUserInfo_whenUserExists_shouldReturnUserInfo() {
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
        when(resourceService.getUserPermissions(1L)).thenReturn(List.of("system:user:list"));

        LoginVO result = authService.getCurrentUserInfo();

        assertNotNull(result);
        assertEquals("mock-token", result.getToken());
        assertNotNull(result.getUser());
    }

    @Test
    @DisplayName("getCurrentUserInfo: 用户不存在时抛出 USER_NOT_FOUND")
    void getCurrentUserInfo_whenUserNotExists_shouldThrowException() {
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(999999L);
        when(userMapper.selectById(999999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUserInfo());
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== changePassword ====================

    @Test
    @DisplayName("changePassword: 旧密码正确时修改成功")
    void changePassword_whenOldPasswordCorrect_shouldSuccess() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("new_password");

        assertDoesNotThrow(() -> authService.changePassword(1L, dto));
        verify(userMapper).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword: 旧密码错误时抛出 OLD_PASSWORD_ERROR")
    void changePassword_whenOldPasswordWrong_shouldThrowException() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrong_password");
        dto.setNewPassword("new_password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(1L, dto));
        assertEquals(ErrorCodeEnum.OLD_PASSWORD_ERROR.getCode(), ex.getCode());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword: 用户不存在时抛出 USER_NOT_FOUND")
    void changePassword_whenUserNotExists_shouldThrowException() {
        when(userMapper.selectById(999999L)).thenReturn(null);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("new_password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(999999L, dto));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("changePassword: 新旧密码相同时抛出 NEW_PASSWORD_SAME_AS_OLD")
    void changePassword_whenSameAsOld_shouldThrowException() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(1L, dto));
        assertEquals(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD.getCode(), ex.getCode());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    // ==================== 发送验证码 ====================

    @Test
    @DisplayName("sendLoginCaptcha: 调用 CaptchaService（LOGIN 场景）")
    void sendLoginCaptcha_shouldCallSendCaptcha() {
        SendCaptchaDTO dto = new SendCaptchaDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");

        doNothing().when(captchaService).sendCaptcha(anyString(), anyString(), anyString());

        authService.sendLoginCaptcha(dto);

        verify(captchaService).sendCaptcha("PHONE", "13800000001", "login");
    }

    @Test
    @DisplayName("sendForgotPasswordCaptcha: target 未注册时静默成功（防枚举）")
    void sendForgotPasswordCaptcha_whenTargetNotExists_shouldSilentlySucceed() {
        SendCaptchaDTO dto = new SendCaptchaDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13999999999");

        when(userMapper.selectByPhone("13999999999")).thenReturn(null);

        authService.sendForgotPasswordCaptcha(dto);

        verify(captchaService, never()).sendCaptcha(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendForgotPasswordCaptcha: target 已注册时调用 CaptchaService（FORGOT 场景）")
    void sendForgotPasswordCaptcha_whenTargetExists_shouldSendCaptcha() {
        SendCaptchaDTO dto = new SendCaptchaDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");

        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        doNothing().when(captchaService).sendCaptcha(anyString(), anyString(), anyString());

        authService.sendForgotPasswordCaptcha(dto);

        verify(captchaService).sendCaptcha("PHONE", "13800000001", "forgot");
    }

    // ==================== 重置密码 ====================

    @Test
    @DisplayName("resetPassword: 验证码正确且用户存在时重置成功并解除锁定")
    void resetPassword_whenValid_shouldUpdatePasswordAndUnlock() {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");
        dto.setCaptcha("123456");
        dto.setNewPassword("newPass123");

        testUser.setLoginFailCount(5);
        testUser.setLockTime(LocalDateTime.now());

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.resetPassword(dto));

        // 验证密码更新且锁定状态被清除
        verify(userMapper).updateById(argThat((UserEntity u) ->
                "encoded".equals(u.getPassword()) &&
                u.getLoginFailCount() == 0 &&
                u.getLockTime() == null
        ));
    }

    @Test
    @DisplayName("resetPassword: 用户不存在时抛出 USER_NOT_FOUND")
    void resetPassword_whenUserNotFound_shouldThrow() {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13999999999");
        dto.setCaptcha("123456");
        dto.setNewPassword("newPass123");

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByPhone("13999999999")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.resetPassword(dto));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("resetPassword(EMAIL): 邮箱场景重置密码成功")
    void resetPassword_byEmail_whenValid_shouldSuccess() {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setCaptchaType(CaptchaTypeEnum.EMAIL);
        dto.setTarget("admin@example.com");
        dto.setCaptcha("654321");
        dto.setNewPassword("newPass456");

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByEmail("admin@example.com")).thenReturn(testUser);
        when(passwordEncoder.encode("newPass456")).thenReturn("encoded2");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.resetPassword(dto));
        verify(userMapper).updateById(argThat((UserEntity u) -> "encoded2".equals(u.getPassword())));
    }
}
