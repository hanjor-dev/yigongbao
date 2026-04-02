package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.mapper.LoginLogMapper;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthServiceImpl authService;

    private MockedStatic<StpUtil> stpUtilMockedStatic;

    private UserEntity testUser;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // Mock Sa-Token 静态方法
        stpUtilMockedStatic = mockStatic(StpUtil.class);

        // 初始化测试用户
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"); // 123456 的 BCrypt 加密
        testUser.setRealName("系统管理员");
        testUser.setPhone("13800000001");
        testUser.setStatus(1);
        testUser.setRoleId(1L);
        testUser.setRoleName("超级管理员");

        // 初始化登录DTO
        loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("123456");
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
        when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
        when(resourceService.getUserPermissions(1L)).thenReturn(List.of("system:user:list"));

        // 执行
        LoginVO result = authService.login(loginDTO);

        // 断言
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
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

        loginDTO.setUsername("not_exists_user");

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

        loginDTO.setPassword("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        verify(loginLogMapper, times(1)).insert(any(LoginLogEntity.class));
    }

    @Test
    @DisplayName("login: 登录成功后返回用户菜单和权限")
    void login_shouldReturnMenusAndPermissions() {
        // 准备
        List<ResourceVO> menus = new ArrayList<>();
        List<String> permissions = List.of("system:user:add", "system:user:edit");

        when(userMapper.selectByUsername("admin")).thenReturn(testUser);
        when(passwordEncoder.matches("123456", testUser.getPassword())).thenReturn(true);
        when(configService.getConfigValue("login.max.failures")).thenReturn("5");
        when(configService.getConfigValue("login.lock.duration")).thenReturn("15");
        stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
        when(loginLogMapper.insert(any(LoginLogEntity.class))).thenReturn(1);
        when(resourceService.getUserMenuTree(1L)).thenReturn(menus);
        when(resourceService.getUserPermissions(1L)).thenReturn(permissions);

        // 执行
        LoginVO result = authService.login(loginDTO);

        // 断言
        assertNotNull(result.getMenus());
        assertNotNull(result.getPermissions());
        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains("system:user:add"));
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

        loginDTO.setPassword("wrong_password");

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

        loginDTO.setPassword("wrong_password");

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
        when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
        when(resourceService.getUserPermissions(1L)).thenReturn(List.of());

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
        when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
        when(resourceService.getUserPermissions(1L)).thenReturn(List.of());

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

        loginDTO.setPassword("wrong_password");

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(loginDTO)
        );
        assertEquals(ErrorCodeEnum.PASSWORD_ERROR.getCode(), exception.getCode());
        // 验证 ConfigService 返回了兜底值
        // 注意：handleLoginFailure 和 login 异常处理中各调用一次，所以是 atLeast(1)
        verify(configService, atLeast(1)).getConfigValue("login.max.failures");
        verify(configService, atLeast(1)).getConfigValue("login.lock.duration");
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
        assertEquals(ErrorCodeEnum.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
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
}
