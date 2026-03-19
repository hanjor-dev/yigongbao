package com.yigongbao.module.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.DataScopeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.mapper.RoleMapper;
import com.yigongbao.module.system.role.vo.RoleVO;
import com.yigongbao.module.system.resource.mapper.RoleResourceMapper;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoleService 单元测试
 *
 * @author hanjor
 * @date 2026-03-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("RoleService 单元测试")
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ResourceService resourceService;

    @Mock
    private RoleResourceMapper roleResourceMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private RoleEntity testEntity;
    private CreateRoleDTO createDTO;
    private UpdateRoleDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 roleMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(roleService, roleMapper);

        LocalDateTime now = LocalDateTime.now();

        // 初始化测试角色实体
        testEntity = new RoleEntity();
        testEntity.setId(1L);
        testEntity.setRoleName("管理员");
        testEntity.setRoleCode("ROLE_ADMIN");
        testEntity.setRoleDesc("系统管理员");
        testEntity.setAccountType(1);
        testEntity.setDataScope(1);
        testEntity.setStatus(1);
        testEntity.setCreateTime(now);
        testEntity.setUpdateTime(now);

        // 初始化创建DTO
        createDTO = new CreateRoleDTO();
        createDTO.setRoleName("测试角色");
        createDTO.setRoleCode("ROLE_TEST");
        createDTO.setRoleDesc("测试角色描述");
        createDTO.setAccountType(1);
        createDTO.setDataScope(2);

        // 初始化更新DTO
        updateDTO = new UpdateRoleDTO();
        updateDTO.setRoleName("更新后的角色名");
        updateDTO.setRoleDesc("更新后的描述");
        updateDTO.setDataScope(3);
    }

    // ==================== listRole 测试 ====================

    @Test
    @DisplayName("listRole: 分页查询成功")
    void listRole_shouldReturnPageData() {
        // 准备
        Page<RoleEntity> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(testEntity));

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 执行
        IPage<RoleVO> result = roleService.listRole(1, 10, null, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(roleMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listRole: 无数据时返回空列表")
    void listRole_whenNoData_shouldReturnEmptyList() {
        // 准备
        Page<RoleEntity> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(Collections.emptyList());

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 执行
        IPage<RoleVO> result = roleService.listRole(1, 10, null, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getRoleById 测试 ====================

    @Test
    @DisplayName("getRoleById: 存在数据时返回VO")
    void getRoleById_whenExists_shouldReturnData() {
        // 准备
        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceService.getResourceIdsByRoleId(1L)).thenReturn(List.of(101L, 102L));

        // 执行
        RoleVO result = roleService.getRoleById(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("管理员", result.getRoleName());
        assertEquals(List.of(101L, 102L), result.getResourceIds());
    }

    @Test
    @DisplayName("getRoleById: 数据不存在时抛出异常")
    void getRoleById_whenNotExists_shouldThrowException() {
        // 准备
        when(roleMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.getRoleById(999L)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createRole 测试 ====================

    @Test
    @DisplayName("createRole: 创建成功")
    void createRole_shouldSuccess() {
        // 准备
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.insert(any(RoleEntity.class))).thenReturn(1);

        // 执行
        roleService.createRole(createDTO);

        // 断言
        verify(roleMapper, times(1)).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("createRole: 角色编码已存在时抛出异常")
    void createRole_whenRoleCodeExists_shouldThrowException() {
        // 准备：角色编码已存在
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(createDTO)
        );
        assertEquals(ErrorCodeEnum.ROLE_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createRole: 未传数据范围时使用默认值（本机构）")
    void createRole_whenDataScopeNotProvided_shouldUseDefault() {
        // 准备：不传dataScope
        CreateRoleDTO dtoNoDataScope = new CreateRoleDTO();
        dtoNoDataScope.setRoleName("测试角色");
        dtoNoDataScope.setRoleCode("ROLE_TEST2");
        dtoNoDataScope.setAccountType(1);

        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.insert(any(RoleEntity.class))).thenReturn(1);

        // 执行
        roleService.createRole(dtoNoDataScope);

        // 断言
        verify(roleMapper, times(1)).insert(any(RoleEntity.class));
    }

    // ==================== updateRole 测试 ====================

    @Test
    @DisplayName("updateRole: 更新成功")
    void updateRole_shouldSuccess() {
        // 准备
        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.updateById(any(RoleEntity.class))).thenReturn(1);

        // 执行
        roleService.updateRole(1L, updateDTO);

        // 断言
        verify(roleMapper, times(1)).updateById(any(RoleEntity.class));
    }

    @Test
    @DisplayName("updateRole: 角色不存在时抛出异常")
    void updateRole_whenNotExists_shouldThrowException() {
        // 准备
        when(roleMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.updateRole(999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateRole: 角色编码与其他角色重复时抛出异常")
    void updateRole_whenRoleCodeDuplicated_shouldThrowException() {
        // 准备：更新后的角色编码与其他角色重复
        UpdateRoleDTO dtoWithCode = new UpdateRoleDTO();
        dtoWithCode.setRoleCode("ROLE_DESIGNER");

        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        // 角色编码检查：存在重复（排除自己后还有1条）
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.updateRole(1L, dtoWithCode)
        );
        assertEquals(ErrorCodeEnum.ROLE_EXISTS.getCode(), exception.getCode());
    }

    // ==================== removeRole 测试 ====================

    @Test
    @DisplayName("removeRole: 删除成功")
    void removeRole_shouldSuccess() {
        // 准备：没有关联用户
        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.countByRoleId(1L)).thenReturn(0L);
        when(roleMapper.deleteById(1L)).thenReturn(1);

        // 执行
        roleService.removeRole(1L);

        // 断言
        verify(roleMapper, times(1)).deleteById(1L);
        verify(roleResourceMapper, times(1)).deleteByRoleId(1L);
    }

    @Test
    @DisplayName("removeRole: 角色不存在时抛出异常")
    void removeRole_whenNotExists_shouldThrowException() {
        // 准备
        when(roleMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.removeRole(999L)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("removeRole: 有关联用户时抛出异常")
    void removeRole_whenHasUsers_shouldThrowException() {
        // 准备：有关联用户
        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        when(userMapper.countByRoleId(1L)).thenReturn(5L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.removeRole(1L)
        );
        assertEquals(ErrorCodeEnum.ROLE_HAS_USERS.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        // 准备
        when(roleMapper.selectById(1L)).thenReturn(testEntity);
        when(roleMapper.updateById(any(RoleEntity.class))).thenReturn(1);

        // 执行
        roleService.updateStatus(1L, StatusConstants.DISABLED);

        // 断言
        verify(roleMapper, times(1)).updateById(any(RoleEntity.class));
    }

    @Test
    @DisplayName("updateStatus: 角色不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        // 准备
        when(roleMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.updateStatus(999L, 0)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }
}
