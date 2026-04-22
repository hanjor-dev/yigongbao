package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.vo.GraphicCaptchaVO;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
 * @date 2026-03-19
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
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private AuthServiceImpl authService;

    private MockedStatic<StpUtil> stpUtilMockedStatic;

    private UserEntity testUser;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // Mock Sa-Token 静态方法
        stpUtilMockedStatic = mockStatic(StpUtil.class);

        // Mock Redis valueOps（图形验证码使用）
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // 默认：图形验证码存在且匹配（PASSWORD 登录测试公共前提）
        when(valueOps.get(startsWith("graphic:captcha:"))).thenReturn("abcd");
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

        // 初始化登录DTO（PASSWORD 类型）
        loginDTO = new LoginDTO();
        loginDTO.setLoginType(LoginTypeEnum.PASSWORD);
        loginDTO.setPrincipal("admin");
        loginDTO.setCredential("123456");
        loginDTO.setCaptchaId("test-captcha-id");
        loginDTO.setCaptchaCode("abcd");
    }

    @AfterEach
    void tearDown() {
        // 关闭静态方法 Mock
        if (stpUtilMockedStatic != null) {
            stpUtilMockedStatic.close();
        }
    }

    // ==================== login 测试 ====================

    @Test
    @DisplayName("login: 用户名密码正确时登录成功")
    void login_whenSuccess_shouldReturnToken() {
        // 准备
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        // 执行
        LoginVO result = authService.login(loginDTO);

        // 断言
        assertNotNull(result);
        assertEquals("mock-token", result.getToken());
        verify(userMapper, times(1)).selectByUsername("admin");
        verify(passwordEncoder, times(1)).matches("123456", testUser.getPassword());
        verify(loginLogMapper, times(1)).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login: 用户名不存在时抛出异常")
    void login_whenUsernameNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectByUsername("not_exists_user")).thenReturn(null);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        loginDTO.setPrincipal("not_exists_user");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR.getCode(), exception.getCode());
        verify(loginLogMapper, times(1)).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login: 用户已禁用时抛出异常")
    void login_whenUserDisabled_shouldThrowException() {
        // 准备
        testUser.setStatus(StatusConstants.DISABLED); // 已禁用
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.USER_DISABLED.getCode(), exception.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login: 密码错误时抛出异常")
    void login_whenPasswordWrong_shouldThrowException() {
        // 准备
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        loginDTO.setCredential("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        verify(loginLogMapper, times(1)).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login: 登录成功后返回 token")
    void login_shouldReturnToken() {
        // 准备
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        // 执行
        LoginVO result = authService.login(loginDTO);

        // 断言
        assertNotNull(result.getToken());
        assertEquals("mock-token", result.getToken());
    }

    @Test
    @DisplayName("login: 账户已锁定时拒绝登录")
    void login_whenAccountLocked_shouldThrowException() {
        // 准备：用户已锁定
        testUser.setLoginFailCount(5);
        testUser.setLockTime(LocalDateTime.now()); // 刚刚锁定
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.ACCOUNT_LOCKED.getCode(), exception.getCode());
        verify(passwordEncoder, never()).matches(any(), any()); // 不应校验密码
        verify(loginLogMapper, times(1)).insert(any(LoginLogEntity.class)); // 记录锁定日志
    }

    @Test
    @DisplayName("login: 密码错误时递增失败计数")
    void login_whenPasswordWrong_shouldIncrementFailCount() {
        // 准备：当前失败计数为3
        testUser.setLoginFailCount(3);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        loginDTO.setCredential("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        // 验证失败计数从3增加到4，但未达到阈值5，未触发锁定
        verify(userMapper, times(1)).updateById(argThat((UserEntity user) ->
                user.getLoginFailCount() != null && user.getLoginFailCount() == 4 &&
                        user.getLockTime() == null
        ));
    }

    @Test
    @DisplayName("login: 密码连续错误达到阈值时锁定账户")
    void login_whenFailCountReachesMax_shouldLockAccount() {
        // 准备：当前失败计数为4，再错1次就达到阈值5
        testUser.setLoginFailCount(4);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        loginDTO.setCredential("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        // 验证失败计数增加到5，且 lockTime 被设置为当前时间（账户锁定）
        verify(userMapper, times(1)).updateById(argThat((UserEntity user) ->
                user.getLoginFailCount() != null && user.getLoginFailCount() == 5 &&
                        user.getLockTime() != null
        ));
    }

    @Test
    @DisplayName("login: 账户锁定超时后自动解锁可正常登录")
    void login_whenLockExpired_shouldAllowLogin() {
        // 准备：lockTime 已超时（30分钟前锁定，锁定时长15分钟）
        testUser.setLoginFailCount(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(30)); // 已超时
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        // 执行：锁定已超时，可正常登录
        LoginVO result = authService.login(loginDTO);

        // 断言：登录成功
        assertNotNull(result);
        assertNotNull(result.getToken());
    }

    @Test
    @DisplayName("login: 登录成功后重置失败计数")
    void login_whenSuccess_shouldResetFailCount() {
        // 准备：当前有失败计数（未锁定）
        testUser.setLoginFailCount(3);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        // 执行
        LoginVO result = authService.login(loginDTO);

        // 断言：登录成功
        assertNotNull(result);
        // 验证失败计数被重置为0，lockTime被清空
        verify(userMapper, times(1)).updateById(argThat((UserEntity user) ->
                user.getLoginFailCount() != null && user.getLoginFailCount() == 0 &&
                        user.getLockTime() == null
        ));
    }

    @Test
    @DisplayName("login: 配置参数为空时使用默认值")
    void login_whenConfigEmpty_shouldUseDefaultValue() {
        // 准备：ConfigService 直接返回默认值（已内置兜底逻辑）
        testUser.setLoginFailCount(4);
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);
        // ConfigService 在数据库无值时直接返回 yigongbao.config 的兜底值
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        loginDTO.setCredential("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        // 验证 ConfigService 被调用获取最大失败次数
        verify(configService, atLeast(1)).getConfigValue("login.max.failures");
    }

    // ==================== logout 测试 ====================

    @Test
    @DisplayName("logout: 登录后登出成功")
    void logout_whenLoggedIn_shouldSuccess() {
        // 准备：模拟已登录
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        // 执行
        assertDoesNotThrow(() -> authService.logout());

        // 验证
        stpUtilMockedStatic.verify(StpUtil::logout, times(1));
    }

    // ==================== changePassword 测试 ====================

    @Test
    @DisplayName("changePassword: 旧密码正确时修改成功")
    void changePassword_whenOldPasswordCorrect_shouldSuccess() {
        // 准备
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("new_password");

        // 执行
        assertDoesNotThrow(() -> authService.changePassword(1L, dto));

        // 验证
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword: 旧密码错误时抛出异常")
    void changePassword_whenOldPasswordWrong_shouldThrowException() {
        // 准备
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", testUser.getPassword())).thenReturn(false);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrong_password");
        dto.setNewPassword("new_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.changePassword(1L, dto)
        );
        assertEquals(ErrorCodeEnum.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword: 用户不存在时抛出异常")
    void changePassword_whenUserNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999999L)).thenReturn(null);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("new_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.changePassword(999999L, dto)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("changePassword: 新旧密码相同时抛出异常")
    void changePassword_whenSameAsOld_shouldThrowException() {
        // 准备
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        // 旧密码校验通过，但新旧密码相同也会通过 matches
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("123456"); // 与旧密码相同

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.changePassword(1L, dto)
        );
        assertEquals(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD.getCode(), exception.getCode());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    // ==================== getCurrentUserInfo 测试 ====================

    @Test
    @DisplayName("getCurrentUserInfo: 用户存在时返回用户信息")
    void getCurrentUserInfo_whenUserExists_shouldReturnUserInfo() {
        // 准备
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
        when(resourceService.getUserPermissions(1L)).thenReturn(List.of("system:user:list"));

        // 执行
        LoginVO result = authService.getCurrentUserInfo();

        // 断言
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
        assertEquals("mock-token", result.getToken());
    }

    @Test
    @DisplayName("getCurrentUserInfo: 用户不存在时抛出异常")
    void getCurrentUserInfo_whenUserNotExists_shouldThrowException() {
        // 准备
        stpUtilMockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(999999L);
        when(userMapper.selectById(999999L)).thenReturn(null);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.getCurrentUserInfo()
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== 图形验证码测试 ====================

    @Test
    @DisplayName("getGraphicCaptcha: 返回非空 captchaId 和 imageBase64")
    void getGraphicCaptcha_shouldReturnValidVO() {
        GraphicCaptchaVO vo = authService.getGraphicCaptcha();
        assertNotNull(vo.getCaptchaId());
        assertNotNull(vo.getImageBase64());
        assertTrue(vo.getImageBase64().startsWith("data:image/png;base64,"));
        verify(valueOps, times(1)).set(startsWith("graphic:captcha:"), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("login(PASSWORD): 图形验证码已过期时抛出 CAPTCHA_GRAPHIC_EXPIRED")
    void login_whenGraphicCaptchaExpired_shouldThrowExpired() {
        when(valueOps.get(startsWith("graphic:captcha:"))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("login(PASSWORD): 图形验证码错误时抛出 CAPTCHA_GRAPHIC_ERROR")
    void login_whenGraphicCaptchaWrong_shouldThrowError() {
        when(valueOps.get(startsWith("graphic:captcha:"))).thenReturn("xxxx");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
        assertEquals(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR.getCode(), ex.getCode());
    }

    // ==================== PHONE/EMAIL 登录测试 ====================

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
        verify(captchaService, times(1)).verifyCaptcha(anyString(), eq("13800000001"), anyString(), anyString());
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
    @DisplayName("login(EMAIL): 邮箱验证码正确时登录成功")
    void login_byEmail_whenSuccess_shouldReturnToken() {
        LoginDTO dto = new LoginDTO();
        dto.setLoginType(LoginTypeEnum.EMAIL);
        dto.setPrincipal("admin@example.com");
        dto.setCredential("123456");

        doNothing().when(captchaService).verifyCaptcha(anyString(), eq("admin@example.com"), anyString(), anyString());
        when(userMapper.selectByEmail("admin@example.com")).thenReturn(testUser);
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);

        LoginVO result = authService.login(dto);
        assertNotNull(result.getToken());
    }

    // ==================== 发送验证码测试 ====================

    @Test
    @DisplayName("sendLoginCaptcha: 调用 CaptchaService.sendCaptcha（LOGIN 场景）")
    void sendLoginCaptcha_shouldCallSendCaptcha() {
        SendCaptchaDTO dto = new SendCaptchaDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");

        doNothing().when(captchaService).sendCaptcha(anyString(), anyString(), anyString());
        authService.sendLoginCaptcha(dto);

        verify(captchaService, times(1)).sendCaptcha("PHONE", "13800000001", "login");
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
    @DisplayName("sendForgotPasswordCaptcha: target 已注册时调用 CaptchaService")
    void sendForgotPasswordCaptcha_whenTargetExists_shouldSendCaptcha() {
        SendCaptchaDTO dto = new SendCaptchaDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");

        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        doNothing().when(captchaService).sendCaptcha(anyString(), anyString(), anyString());
        authService.sendForgotPasswordCaptcha(dto);

        verify(captchaService, times(1)).sendCaptcha("PHONE", "13800000001", "forgot");
    }

    // ==================== 重置密码测试 ====================

    @Test
    @DisplayName("resetPassword: 验证码正确且用户存在时重置成功")
    void resetPassword_whenValid_shouldUpdatePassword() {
        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setCaptchaType(CaptchaTypeEnum.PHONE);
        dto.setTarget("13800000001");
        dto.setCaptcha("123456");
        dto.setNewPassword("newPass123");

        doNothing().when(captchaService).verifyCaptcha(anyString(), anyString(), anyString(), anyString());
        when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.resetPassword(dto));
        verify(userMapper, times(1)).updateById(argThat((UserEntity u) -> "encoded".equals(u.getPassword())));
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
}
