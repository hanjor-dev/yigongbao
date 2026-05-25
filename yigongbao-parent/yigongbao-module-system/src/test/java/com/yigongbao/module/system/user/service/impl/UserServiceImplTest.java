package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.dto.ChangePasswordDTO;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.dto.UserPageDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.vo.UserVO;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 *
 * @author hanjor
 * @date 2026-03-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("UserService 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrgService orgService;

    @Mock
    private DeptService deptService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ConfigService configService;

    @Mock
    private UserHospitalService userHospitalService;

    @Mock
    private DictService dictService;

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @Mock
    private com.yigongbao.module.system.dept.mapper.DeptOrgMapper deptOrgMapper;

    @Mock
    private com.yigongbao.module.basic.code.service.CodeGeneratorService codeGeneratorService;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity testEntity;
    private CreateUserDTO createDTO;
    private UpdateUserDTO updateDTO;
    private UpdateUserBySelfDTO updateSelfDTO;
    private OrgEntity testOrg;
    private DeptEntity testDept;
    private RoleEntity testRole;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 userMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(userService, userMapper);

        LocalDateTime now = LocalDateTime.now();

        // 初始化测试机构实体
        testOrg = new OrgEntity();
        testOrg.setId(1L);
        testOrg.setOrgName("测试机构");
        testOrg.setOrgCode("ORG-P-001");
        testOrg.setStatus(1);

        // 初始化测试部门实体
        testDept = new DeptEntity();
        testDept.setId(1L);
        testDept.setDeptName("研发部");
        testDept.setDeptCode("DEPT-001");
        testDept.setDeptType("6.2");
        testDept.setStatus(1);

        // 初始化测试角色实体
        testRole = new RoleEntity();
        testRole.setId(1L);
        testRole.setRoleName("管理员");
        testRole.setRoleCode("ROLE_ADMIN");
        testRole.setDataScopeType("all");
        testRole.setStatus(1);

        // 初始化测试用户实体
        testEntity = new UserEntity();
        testEntity.setId(1L);
        testEntity.setUsername("testuser");
        testEntity.setPassword("$2a$10$xxx");
        testEntity.setRealName("测试用户");
        testEntity.setPhone("13800000001");
        testEntity.setAccountType("6.1");
        testEntity.setOrgId(1L);
        testEntity.setDeptId(1L);
        testEntity.setRoleId(1L);
        testEntity.setStatus(1);
        testEntity.setCreateTime(now);
        testEntity.setUpdateTime(now);

        // 初始化创建DTO（密码需包含字母和数字，符合密码强度要求）
        createDTO = new CreateUserDTO();
        createDTO.setUsername("newuser");
        createDTO.setPassword("test123");
        createDTO.setRealName("新用户");
        createDTO.setPhone("13900000000");
        createDTO.setAccountType("6.1");
        createDTO.setOrgId(1L);
        createDTO.setDeptId(1L);
        createDTO.setRoleId(1L);

        // 初始化更新DTO
        updateDTO = new UpdateUserDTO();
        updateDTO.setRealName("更新后的姓名");
        updateDTO.setEmail("update@test.com");

        // 初始化自更新DTO
        updateSelfDTO = new UpdateUserBySelfDTO();
        updateSelfDTO.setPhone("13900000999");
        updateSelfDTO.setAvatar("/avatar/new.png");
    }

    // ==================== listUser 测试 ====================

    @Test
    @DisplayName("listUser: 分页查询成功")
    void listUser_shouldReturnPageData() {
        // 准备
        Page<UserEntity> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(testEntity));

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptService.getById(1L)).thenReturn(testDept);
        when(roleService.getById(1L)).thenReturn(testRole);

        // 执行
        UserPageDTO pageDTO1 = new UserPageDTO();
        pageDTO1.setPageNum(1);
        pageDTO1.setPageSize(10);
        IPage<UserVO> result = userService.listUser(pageDTO1);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(userMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listUser: 无数据时返回空列表")
    void listUser_whenNoData_shouldReturnEmptyList() {
        // 准备
        Page<UserEntity> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(Collections.emptyList());

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 执行
        UserPageDTO pageDTO2 = new UserPageDTO();
        pageDTO2.setPageNum(1);
        pageDTO2.setPageSize(10);
        IPage<UserVO> result = userService.listUser(pageDTO2);

        // 断言
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getUserById 测试 ====================

    @Test
    @DisplayName("getUserById: 存在数据时返回VO")
    void getUserById_whenExists_shouldReturnData() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptService.getById(1L)).thenReturn(testDept);
        when(roleService.getById(1L)).thenReturn(testRole);

        // 执行
        UserVO result = userService.getUserById(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试用户", result.getRealName());
    }

    @Test
    @DisplayName("getUserById: 数据不存在时抛出异常")
    void getUserById_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.getUserById(999L)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createUser 测试 ====================

    @Test
    @DisplayName("createUser: 创建成功")
    void createUser_shouldSuccess() {
        // 准备
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptService.getById(1L)).thenReturn(testDept);
        when(roleService.getById(1L)).thenReturn(testRole);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.createUser(createDTO);

        // 断言
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUser: 用户名已存在时抛出异常")
    void createUser_whenUsernameExists_shouldThrowException() {
        // 准备：用户名已存在
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(createDTO)
        );
        assertEquals(ErrorCodeEnum.USER_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 手机号已存在时抛出异常")
    void createUser_whenPhoneExists_shouldThrowException() {
        // 准备：用户名不存在，手机号存在
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // 用户名检查
                .thenReturn(1L); // 手机号检查

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(createDTO)
        );
        assertEquals(ErrorCodeEnum.USER_PHONE_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 邮箱已存在时抛出异常")
    void createUser_whenEmailExists_shouldThrowException() {
        // 准备：用户名不存在，手机号不存在，邮箱存在
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // 用户名检查
                .thenReturn(0L)  // 手机号检查
                .thenReturn(1L); // 邮箱检查
        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("newuser");
        dto.setPhone("13900000000");
        dto.setEmail("exists@example.com");
        dto.setAccountType("6.1");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_EMAIL_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateUser: 邮箱已存在时抛出异常")
    void updateUser_whenEmailExists_shouldThrowException() {
        // 准备：被更新的用户邮箱不同，且邮箱已被其他用户使用
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setEmail("old@example.com");
        existing.setPhone("13800000000");
        existing.setRoleId(1L);
        when(userMapper.selectById(1L)).thenReturn(existing);
        // email check — returns 1L meaning exists
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setEmail("new@example.com");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(1L, dto)
        );
        assertEquals(ErrorCodeEnum.USER_EMAIL_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 所属机构不存在时抛出异常")
    void createUser_whenOrgNotExists_shouldThrowException() {
        // 准备：用户名和手机号不存在，但机构不存在
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(999L)).thenReturn(null);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("newuser");
        dto.setPhone("13900000000");
        dto.setOrgId(999L);
        dto.setAccountType("6.1");

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_ORG_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 所属部门不存在时抛出异常")
    void createUser_whenDeptNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptService.getById(999L)).thenReturn(null);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("newuser");
        dto.setPhone("13900000000");
        dto.setOrgId(1L);
        dto.setDeptId(999L);
        dto.setAccountType("6.1");

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 角色不存在时抛出异常")
    void createUser_whenRoleNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptService.getById(1L)).thenReturn(testDept);
        when(roleService.getById(999L)).thenReturn(null);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("newuser");
        dto.setPhone("13900000000");
        dto.setOrgId(1L);
        dto.setDeptId(1L);
        dto.setRoleId(999L);
        dto.setAccountType("6.1");

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_ROLE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 密码可选（不传密码）使用默认密码")
    void createUser_whenPasswordOptional_shouldUseDefaultPassword() {
        // 准备：不传密码
        CreateUserDTO dtoNoPassword = new CreateUserDTO();
        dtoNoPassword.setUsername("newuser");
        dtoNoPassword.setRealName("新用户");
        dtoNoPassword.setPhone("13900000000");
        dtoNoPassword.setAccountType("6.1");
        dtoNoPassword.setOrgId(1L);

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        // Mock 配置服务返回默认密码
        when(configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey())).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.createUser(dtoNoPassword);

        // 断言：验证使用了数据库配置密码
        verify(configService, times(1)).getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
        verify(passwordEncoder, times(1)).encode("123456");
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUser: 密码可选（配置不存在）使用兜底默认值")
    void createUser_whenConfigNotFound_shouldUseFallbackPassword() {
        // 准备：不传密码，且配置服务返回兜底默认值（ConfigService 已内置兜底逻辑）
        CreateUserDTO dtoNoPassword = new CreateUserDTO();
        dtoNoPassword.setUsername("newuser");
        dtoNoPassword.setRealName("新用户");
        dtoNoPassword.setPhone("13900000000");
        dtoNoPassword.setAccountType("6.1");
        dtoNoPassword.setOrgId(1L);

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        // ConfigService 在数据库无值时直接返回 yigongbao.config 的兜底值
        when(configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey())).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.createUser(dtoNoPassword);

        // 断言：验证使用了兜底默认值
        verify(configService, times(1)).getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
        verify(passwordEncoder, times(1)).encode("123456");
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    // ==================== updateUser 测试 ====================

    @Test
    @DisplayName("updateUser: 更新成功")
    void updateUser_shouldSuccess() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateUser(1L, updateDTO);

        // 断言
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("updateUser: 用户不存在时抛出异常")
    void updateUser_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateUser: 手机号与其他用户重复时抛出异常")
    void updateUser_whenPhoneDuplicated_shouldThrowException() {
        // 准备：更新后的手机号与其他用户重复
        UpdateUserDTO dtoWithPhone = new UpdateUserDTO();
        dtoWithPhone.setPhone("13800000002");

        when(userMapper.selectById(1L)).thenReturn(testEntity);
        // 手机号检查：存在重复（排除自己后还有1条）
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(1L, dtoWithPhone)
        );
        assertEquals(ErrorCodeEnum.USER_PHONE_EXISTS.getCode(), exception.getCode());
    }

    // ==================== removeUser 测试 ====================

    @Test
    @DisplayName("removeUser: 删除成功")
    void removeUser_shouldSuccess() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.deleteById(1L)).thenReturn(1);

        // 执行
        userService.removeUser(1L);

        // 断言
        verify(userMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("removeUser: 用户不存在时抛出异常")
    void removeUser_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.removeUser(999L)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateStatus(1L, StatusConstants.DISABLED);

        // 断言
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("updateStatus: 用户不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateStatus(999L, 0)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== resetPassword 测试 ====================

    @Test
    @DisplayName("resetPassword: 重置成功")
    void resetPassword_shouldSuccess() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        // Mock 配置服务返回默认密码
        when(configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey())).thenReturn("123456");
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("$2a$10$encrypted");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.resetPassword(1L);

        // 断言
        verify(configService, times(1)).getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("resetPassword: 用户不存在时抛出异常")
    void resetPassword_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.resetPassword(999L)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== changePassword 测试 ====================

    @Test
    @DisplayName("changePassword: 修改密码成功")
    void changePassword_shouldSuccess() {
        // 准备（密码需包含字母和数字，符合密码强度要求）
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(passwordEncoder.matches("old123", "$2a$10$xxx")).thenReturn(true);
        when(passwordEncoder.encode("new456")).thenReturn("$2a$10$newencrypted");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        ChangePasswordDTO cpDTO1 = new ChangePasswordDTO();
        cpDTO1.setOldPassword("old123");
        cpDTO1.setNewPassword("new456");
        userService.changePassword(1L, cpDTO1);

        // 断言
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword: 旧密码错误时抛出异常")
    void changePassword_whenOldPasswordWrong_shouldThrowException() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(passwordEncoder.matches("wrongpassword", "$2a$10$xxx")).thenReturn(false);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> {
                    ChangePasswordDTO cpDTO2 = new ChangePasswordDTO();
                    cpDTO2.setOldPassword("wrongpassword");
                    cpDTO2.setNewPassword("654321");
                    userService.changePassword(1L, cpDTO2);
                }
        );
        assertEquals(ErrorCodeEnum.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("changePassword: 用户不存在时抛出异常")
    void changePassword_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> {
                    ChangePasswordDTO cpDTO3 = new ChangePasswordDTO();
                    cpDTO3.setOldPassword("123456");
                    cpDTO3.setNewPassword("654321");
                    userService.changePassword(999L, cpDTO3);
                }
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateUserBySelf 测试 ====================

    @Test
    @DisplayName("updateUserBySelf: 自更新成功")
    void updateUserBySelf_shouldSuccess() {
        // 准备
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateUserBySelf(1L, updateSelfDTO);

        // 断言
        verify(userMapper, times(1)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("updateUserBySelf: 用户不存在时抛出异常")
    void updateUserBySelf_whenNotExists_shouldThrowException() {
        // 准备
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUserBySelf(999L, updateSelfDTO)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateUserBySelf: 手机号与其他用户重复时抛出异常")
    void updateUserBySelf_whenPhoneDuplicated_shouldThrowException() {
        // 准备：更新后的手机号与其他用户重复
        when(userMapper.selectById(1L)).thenReturn(testEntity);
        // 手机号检查：存在重复（排除自己后还有1条）
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUserBySelf(1L, updateSelfDTO)
        );
        assertEquals(ErrorCodeEnum.USER_PHONE_EXISTS.getCode(), exception.getCode());
    }

    // ==================== createUser 医院范围权限测试 ====================

    /**
     * 场景：角色 dataScopeType=hospitals，创建用户时传入 hospitalIds
     * 期望：调用 userHospitalService.assignHospitals
     */
    @Test
    @DisplayName("createUser: dataScopeType=hospitals时传入hospitalIds应分配医院权限")
    void createUser_whenDataScopeTypeHospitalsAndHospitalIds_shouldAssignHospitals() {
        // 准备：dataScopeType=hospitals 的角色 + 传入 hospitalIds
        RoleEntity roleWithHospitalScope = new RoleEntity();
        roleWithHospitalScope.setId(2L);
        roleWithHospitalScope.setRoleName("业务员");
        roleWithHospitalScope.setDataScopeType("hospitals");
        roleWithHospitalScope.setStatus(1);

        CreateUserDTO dtoWithHospitals = new CreateUserDTO();
        dtoWithHospitals.setUsername("hospitaluser");
        dtoWithHospitals.setRealName("医院用户");
        dtoWithHospitals.setPhone("13900000002");
        dtoWithHospitals.setAccountType("6.1");
        dtoWithHospitals.setOrgId(1L);
        dtoWithHospitals.setRoleId(2L);
        dtoWithHospitals.setHospitalIds(List.of(10L, 20L, 30L));

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(2L)).thenReturn(roleWithHospitalScope);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });

        // 执行
        userService.createUser(dtoWithHospitals);

        // 断言：验证分配了医院权限
        verify(userHospitalService, times(1)).assignHospitals(eq(100L), eq(List.of(10L, 20L, 30L)));
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    /**
     * 场景：角色 dataScopeType=org，创建用户时传入 hospitalIds
     * 期望：不调用 userHospitalService.assignHospitals（角色不支持医院范围权限）
     */
    @Test
    @DisplayName("createUser: dataScopeType=org时传入hospitalIds不分配医院权限")
    void createUser_whenDataScopeTypeOrgAndHospitalIds_shouldNotAssignHospitals() {
        // 准备：dataScopeType=org 的角色 + 传入 hospitalIds
        RoleEntity roleWithoutHospitalScope = new RoleEntity();
        roleWithoutHospitalScope.setId(3L);
        roleWithoutHospitalScope.setRoleName("普通员工");
        roleWithoutHospitalScope.setDataScopeType("org");
        roleWithoutHospitalScope.setStatus(1);

        CreateUserDTO dtoWithHospitals = new CreateUserDTO();
        dtoWithHospitals.setUsername("normaluser");
        dtoWithHospitals.setRealName("普通用户");
        dtoWithHospitals.setPhone("13900000003");
        dtoWithHospitals.setAccountType("6.1");
        dtoWithHospitals.setOrgId(1L);
        dtoWithHospitals.setRoleId(3L);
        dtoWithHospitals.setHospitalIds(List.of(10L, 20L));

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(3L)).thenReturn(roleWithoutHospitalScope);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        });

        // 执行
        userService.createUser(dtoWithHospitals);

        // 断言：验证未分配医院权限
        verify(userHospitalService, never()).assignHospitals(anyLong(), anyList());
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    /**
     * 场景：角色 dataScopeType=hospitals，但未传入 hospitalIds
     * 期望：抛出异常（角色需要医院范围权限时必须分配医院）
     */
    @Test
    @DisplayName("createUser: dataScopeType=hospitals但未传hospitalIds应抛出异常")
    void createUser_whenDataScopeTypeHospitalsWithoutHospitalIds_shouldThrowException() {
        // 准备：dataScopeType=hospitals 的角色，但不传 hospitalIds
        RoleEntity roleWithHospitalScope = new RoleEntity();
        roleWithHospitalScope.setId(2L);
        roleWithHospitalScope.setRoleName("业务员");
        roleWithHospitalScope.setDataScopeType("hospitals");
        roleWithHospitalScope.setStatus(1);

        CreateUserDTO dtoWithoutHospitals = new CreateUserDTO();
        dtoWithoutHospitals.setUsername("nohospitaluser");
        dtoWithoutHospitals.setRealName("无医院用户");
        dtoWithoutHospitals.setPhone("13900000004");
        dtoWithoutHospitals.setAccountType("6.1");
        dtoWithoutHospitals.setOrgId(1L);
        dtoWithoutHospitals.setRoleId(2L);
        // 不设置 hospitalIds

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(2L)).thenReturn(roleWithHospitalScope);

        // 执行 & 断言：应抛出异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dtoWithoutHospitals)
        );
        assertEquals(ErrorCodeEnum.USER_ROLE_HOSPITAL_SCOPE_REQUIRED.getCode(), exception.getCode());
    }

    // ==================== updateUser 医院范围权限测试 ====================

    /**
     * 场景：变更角色为 dataScopeType=hospitals，且传入 hospitalIds
     * 期望：调用 userHospitalService.assignHospitals
     */
    @Test
    @DisplayName("updateUser: 变更角色为dataScopeType=hospitals时传入hospitalIds应分配医院权限")
    void updateUser_whenRoleChangedToDataScopeTypeHospitals_shouldAssignHospitals() {
        // 准备：从 dataScopeType=org 变更到 dataScopeType=hospitals
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPhone("13800000001");
        existingUser.setOrgId(1L);
        existingUser.setRoleId(3L);  // 原角色 dataScopeType=org

        RoleEntity newRoleWithHospitalScope = new RoleEntity();
        newRoleWithHospitalScope.setId(2L);
        newRoleWithHospitalScope.setRoleName("业务员");
        newRoleWithHospitalScope.setDataScopeType("hospitals");
        newRoleWithHospitalScope.setStatus(1);

        UpdateUserDTO dtoWithNewRole = new UpdateUserDTO();
        dtoWithNewRole.setRoleId(2L);
        dtoWithNewRole.setHospitalIds(List.of(10L, 20L));

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleService.getById(2L)).thenReturn(newRoleWithHospitalScope);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateUser(1L, dtoWithNewRole);

        // 断言：验证分配了医院权限
        verify(userHospitalService, times(1)).assignHospitals(eq(1L), eq(List.of(10L, 20L)));
    }

    /**
     * 场景：角色未变更，但医院列表有变更（编辑页单独调整医院）
     * 期望：调用 userHospitalService.assignHospitals
     */
    @Test
    @DisplayName("updateUser: 角色未变更但hospitalIds变更应重新分配医院权限")
    void updateUser_whenRoleUnchangedButHospitalIdsChanged_shouldReassignHospitals() {
        // 准备：用户已有角色 dataScopeType=hospitals，仅变更医院列表
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPhone("13800000001");
        existingUser.setOrgId(1L);
        existingUser.setRoleId(2L);  // dataScopeType=hospitals

        RoleEntity currentRole = new RoleEntity();
        currentRole.setId(2L);
        currentRole.setRoleName("业务员");
        currentRole.setDataScopeType("hospitals");
        currentRole.setStatus(1);

        UpdateUserDTO dtoWithNewHospitals = new UpdateUserDTO();
        dtoWithNewHospitals.setHospitalIds(List.of(99L, 88L));  // 新医院列表

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleService.getById(2L)).thenReturn(currentRole);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateUser(1L, dtoWithNewHospitals);

        // 断言：验证重新分配了医院权限
        verify(userHospitalService, times(1)).assignHospitals(eq(1L), eq(List.of(99L, 88L)));
    }

    /**
     * 场景：变更角色为 dataScopeType=org，但传了 hospitalIds
     * 期望：不调用 userHospitalService.assignHospitals（角色不支持医院范围权限）
     */
    @Test
    @DisplayName("updateUser: 变更角色为dataScopeType=org时传入hospitalIds应忽略不分配")
    void updateUser_whenRoleChangedToDataScopeTypeOrg_shouldNotAssignHospitals() {
        // 准备：从 dataScopeType=hospitals 变更到 dataScopeType=org
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPhone("13800000001");
        existingUser.setOrgId(1L);
        existingUser.setRoleId(2L);  // 原角色 dataScopeType=hospitals

        RoleEntity newRoleWithoutHospitalScope = new RoleEntity();
        newRoleWithoutHospitalScope.setId(1L);
        newRoleWithoutHospitalScope.setRoleName("公司管理员");
        newRoleWithoutHospitalScope.setDataScopeType("all");
        newRoleWithoutHospitalScope.setStatus(1);

        UpdateUserDTO dtoWithNewRole = new UpdateUserDTO();
        dtoWithNewRole.setRoleId(1L);
        dtoWithNewRole.setHospitalIds(List.of(10L, 20L));  // 传了医院ID

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleService.getById(1L)).thenReturn(newRoleWithoutHospitalScope);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        // 执行
        userService.updateUser(1L, dtoWithNewRole);

        // 断言：验证未分配医院权限
        verify(userHospitalService, never()).assignHospitals(anyLong(), anyList());
    }

    /**
     * 场景：新角色 dataScopeType=hospitals，但未传 hospitalIds
     * 期望：不调用 userHospitalService.assignHospitals
     */
    @Test
    @DisplayName("updateUser: 变更角色为dataScopeType=hospitals但未传hospitalIds应抛出异常")
    void updateUser_whenDataScopeTypeHospitalsWithoutHospitalIds_shouldThrowException() {
        // 准备：变更到 dataScopeType=hospitals 的角色，但不传 hospitalIds
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPhone("13800000001");
        existingUser.setOrgId(1L);
        existingUser.setRoleId(3L);  // 原角色 dataScopeType=org

        RoleEntity newRoleWithHospitalScope = new RoleEntity();
        newRoleWithHospitalScope.setId(2L);
        newRoleWithHospitalScope.setRoleName("业务员");
        newRoleWithHospitalScope.setDataScopeType("hospitals");
        newRoleWithHospitalScope.setStatus(1);

        UpdateUserDTO dtoWithoutHospitals = new UpdateUserDTO();
        dtoWithoutHospitals.setRoleId(2L);
        // 不设置 hospitalIds

        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleService.getById(2L)).thenReturn(newRoleWithHospitalScope);

        // 执行 & 断言：应抛出异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(1L, dtoWithoutHospitals)
        );
        assertEquals(ErrorCodeEnum.USER_ROLE_HOSPITAL_SCOPE_REQUIRED.getCode(), exception.getCode());
    }

    /**
     * 场景：用户原有医院，编辑时不传 hospitalIds（不清空）
     * 期望：不调用 userHospitalService.assignHospitals（保持原有关联）
     */
    @Test
    @DisplayName("updateUser: 医院Ids为null时应保持原有关联不调用assignHospitals")
    void updateUser_whenHospitalIdsNull_shouldNotChangeHospitalScope() {
        // 准备：用户已有角色 dataScopeType=hospitals，但编辑时未传 hospitalIds
        UserEntity existingUser = new UserEntity();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPhone("13800000001");
        existingUser.setOrgId(1L);
        existingUser.setRoleId(2L);  // dataScopeType=hospitals

        UpdateUserDTO dtoWithoutHospitals = new UpdateUserDTO();  // hospitalIds=null

        when(userMapper.selectById(1L)).thenReturn(existingUser);

        // 执行
        userService.updateUser(1L, dtoWithoutHospitals);

        // 断言：验证未调用 assignHospitals（保持原有关联）
        verify(userHospitalService, never()).assignHospitals(anyLong(), anyList());
    }

    // ==================== getUserById 医院范围字段测试 ====================

    /**
     * 场景：查询用户详情
     * 期望：填充 dataScopeType 和 hospitalIds
     */
    @Test
    @DisplayName("getUserById: 应填充dataScopeType和hospitalIds")
    void getUserById_shouldFillDataScopeTypeAndHospitalIds() {
        // 准备
        UserEntity userWithRole = new UserEntity();
        userWithRole.setId(1L);
        userWithRole.setUsername("testuser");
        userWithRole.setRealName("测试用户");
        userWithRole.setPhone("13800000001");
        userWithRole.setStatus(1);
        userWithRole.setRoleId(2L);

        RoleEntity roleWithHospitalScope = new RoleEntity();
        roleWithHospitalScope.setId(2L);
        roleWithHospitalScope.setRoleName("业务员");
        roleWithHospitalScope.setDataScopeType("hospitals");

        when(userMapper.selectById(1L)).thenReturn(userWithRole);
        when(roleService.getById(2L)).thenReturn(roleWithHospitalScope);
        when(userHospitalService.getHospitalIdsByUserId(1L)).thenReturn(List.of(10L, 20L));

        // 执行
        UserVO result = userService.getUserById(1L);

        // 断言
        assertNotNull(result);
        assertEquals("hospitals", result.getDataScopeType());
        assertNotNull(result.getHospitalIds());
        assertEquals(2, result.getHospitalIds().size());
        assertTrue(result.getHospitalIds().contains(10L));
        assertTrue(result.getHospitalIds().contains(20L));
    }

    /**
     * 场景：用户无角色
     * 期望：dataScopeType 为 null
     */
    @Test
    @DisplayName("getUserById: 用户无角色时dataScopeType为null")
    void getUserById_whenNoRole_shouldReturnNullDataScopeType() {
        // 准备：无角色用户
        UserEntity userWithoutRole = new UserEntity();
        userWithoutRole.setId(1L);
        userWithoutRole.setUsername("noroleuser");
        userWithoutRole.setRealName("无角色用户");
        userWithoutRole.setPhone("13800000005");
        userWithoutRole.setStatus(1);
        userWithoutRole.setRoleId(null);

        when(userMapper.selectById(1L)).thenReturn(userWithoutRole);
        when(userHospitalService.getHospitalIdsByUserId(1L)).thenReturn(Collections.emptyList());

        // 执行
        UserVO result = userService.getUserById(1L);

        // 断言
        assertNotNull(result);
        assertNull(result.getDataScopeType());
        assertTrue(result.getHospitalIds().isEmpty());
    }

    // ==================== specialty 多选校验测试 ====================

    @Test
    @DisplayName("createUser: 设计师多个合法专业方向，成功")
    void createUser_designerWithMultipleSpecialties_success() {
        RoleEntity designerRole = new RoleEntity();
        designerRole.setId(10L);
        designerRole.setRoleName("设计师");
        designerRole.setRoleCode("designer");
        designerRole.setDataScopeType("all");
        designerRole.setStatus(1);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("designer_new");
        dto.setPassword("test123");
        dto.setRealName("新设计师");
        dto.setPhone("13911111111");
        dto.setAccountType("6.1");
        dto.setOrgId(1L);
        dto.setRoleId(10L);
        dto.setSpecialtyList(List.of("7.1", "7.2"));

        com.yigongbao.module.system.dict.vo.DictVO dict71 = new com.yigongbao.module.system.dict.vo.DictVO();
        dict71.setDictCode("7.1");
        dict71.setDictName("口腔修复");
        com.yigongbao.module.system.dict.vo.DictVO dict72 = new com.yigongbao.module.system.dict.vo.DictVO();
        dict72.setDictCode("7.2");
        dict72.setDictName("种植设计");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(10L)).thenReturn(designerRole);
        when(dictService.getByDictCode("7.1")).thenReturn(dict71);
        when(dictService.getByDictCode("7.2")).thenReturn(dict72);
        when(configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey())).thenReturn("Abc12345");
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$xxx");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        userService.createUser(dto);

        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUser: 设计师专业方向编码格式无效，抛 USER_SPECIALTY_INVALID")
    void createUser_invalidSpecialtyCode_throwsException() {
        RoleEntity designerRole = new RoleEntity();
        designerRole.setId(10L);
        designerRole.setRoleName("设计师");
        designerRole.setRoleCode("designer");
        designerRole.setDataScopeType("all");
        designerRole.setStatus(1);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("designer_new2");
        dto.setPassword("test123");
        dto.setRealName("新设计师2");
        dto.setPhone("13922222222");
        dto.setAccountType("6.1");
        dto.setOrgId(1L);
        dto.setRoleId(10L);
        dto.setSpecialtyList(List.of("invalid"));

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(10L)).thenReturn(designerRole);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_SPECIALTY_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser: 设计师未传专业方向，抛 USER_ROLE_SPECIALTY_REQUIRED")
    void createUser_designerWithoutSpecialty_throwsException() {
        RoleEntity designerRole = new RoleEntity();
        designerRole.setId(10L);
        designerRole.setRoleName("设计师");
        designerRole.setRoleCode("designer");
        designerRole.setDataScopeType("all");
        designerRole.setStatus(1);

        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("designer_new3");
        dto.setPassword("test123");
        dto.setRealName("新设计师3");
        dto.setPhone("13933333333");
        dto.setAccountType("6.1");
        dto.setOrgId(1L);
        dto.setRoleId(10L);
        // specialtyList 为 null

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(roleService.getById(10L)).thenReturn(designerRole);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createUser(dto)
        );
        assertEquals(ErrorCodeEnum.USER_ROLE_SPECIALTY_REQUIRED.getCode(), exception.getCode());
    }
}
