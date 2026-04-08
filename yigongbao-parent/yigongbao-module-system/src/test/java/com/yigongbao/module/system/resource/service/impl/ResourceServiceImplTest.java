package com.yigongbao.module.system.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.resource.dto.CreateResourceDTO;
import com.yigongbao.module.system.resource.dto.UpdateResourceDTO;
import com.yigongbao.module.system.resource.entity.ResourceEntity;
import com.yigongbao.module.system.resource.entity.RoleResourceEntity;
import com.yigongbao.module.system.resource.mapper.ResourceMapper;
import com.yigongbao.module.system.resource.mapper.RoleResourceMapper;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 资源管理 Service 单元测试
 * 使用 Mockito 进行单元测试，不依赖真实数据库
 *
 * @author hanjor
 * @date 2026-03-19
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResourceService 单元测试")
class ResourceServiceImplTest {

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private RoleResourceMapper roleResourceMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private ResourceEntity testEntity;
    private CreateResourceDTO createDTO;
    private UpdateResourceDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 resourceMapper 注入到 ServiceImpl 的 baseMapper 字段中
        // 这是解决 MyBatis-Plus ServiceImpl 继承类单元测试的关键步骤
        Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(resourceService, resourceMapper);

        // 初始化测试数据
        testEntity = new ResourceEntity();
        testEntity.setId(1L);
        testEntity.setParentId(0L);
        testEntity.setResourceName("系统管理");
        testEntity.setResourceCode("system");
        testEntity.setResourceType(1);
        testEntity.setIcon("Setting");
        testEntity.setPath("/system");
        testEntity.setSort(100);
        testEntity.setVisible(1);
        testEntity.setStatus(1);

        // 初始化创建DTO
        createDTO = new CreateResourceDTO();
        createDTO.setParentId(0L);
        createDTO.setResourceName("新资源");
        createDTO.setResourceCode("test:new");
        createDTO.setResourceType(1);
        createDTO.setSort(1);
        createDTO.setVisible(1);
        createDTO.setStatus(1);

        // 初始化更新DTO
        updateDTO = new UpdateResourceDTO();
        updateDTO.setParentId(0L);
        updateDTO.setResourceName("更新资源");
        updateDTO.setResourceCode("test:update");
        updateDTO.setResourceType(1);
        updateDTO.setSort(1);
        updateDTO.setVisible(1);
        updateDTO.setStatus(1);
    }

    // ==================== getResourceById 测试 ====================

    @Test
    @DisplayName("getResourceById: 存在数据时返回VO")
    void getResourceById_whenExists_shouldReturnData() {
        // 准备
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);

        // 执行
        ResourceVO result = resourceService.getResourceById(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("系统管理", result.getResourceName());
        assertEquals("system", result.getResourceCode());
        verify(resourceMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("getResourceById: 数据不存在时抛出异常")
    void getResourceById_whenNotExists_shouldThrowException() {
        // 准备
        when(resourceMapper.selectById(999999L)).thenReturn(null);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.getResourceById(999999L)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
        verify(resourceMapper, times(1)).selectById(999999L);
    }

    // ==================== createResource 测试 ====================

    @Test
    @DisplayName("createResource: 成功创建资源")
    void createResource_shouldSuccess() {
        // 准备
        when(resourceMapper.selectIdByCode("test:new")).thenReturn(null);
        when(resourceMapper.insert(any(ResourceEntity.class))).thenReturn(1);

        // 执行
        assertDoesNotThrow(() -> resourceService.createResource(createDTO));

        // 验证
        verify(resourceMapper, times(1)).selectIdByCode("test:new");
        verify(resourceMapper, times(1)).insert(any(ResourceEntity.class));
    }

    @Test
    @DisplayName("createResource: 资源编码重复时抛出异常")
    void createResource_whenCodeDuplicate_shouldThrowException() {
        // 准备：使用已存在的资源编码
        createDTO.setResourceCode("system");
        when(resourceMapper.selectIdByCode("system")).thenReturn(1L);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.createResource(createDTO)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_EXISTS.getCode(), exception.getCode());
        verify(resourceMapper, times(1)).selectIdByCode("system");
        verify(resourceMapper, never()).insert(any(ResourceEntity.class));
    }

    @Test
    @DisplayName("createResource: 父级资源不存在时抛出异常")
    void createResource_whenParentNotExists_shouldThrowException() {
        // 准备：使用不存在的父级ID（非按钮类型）
        createDTO.setParentId(999999L);
        createDTO.setResourceCode("test:new:child");
        createDTO.setResourceType(2); // 二级菜单
        when(resourceMapper.selectIdByCode("test:new:child")).thenReturn(null);
        when(resourceMapper.selectById(999999L)).thenReturn(null);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.createResource(createDTO)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
        verify(resourceMapper, never()).insert(any(ResourceEntity.class));
    }

    @Test
    @DisplayName("createResource: 按钮类型不校验父级")
    void createResource_buttonType_shouldNotCheckParent() {
        // 准备：按钮类型
        createDTO.setResourceType(3); // 按钮
        createDTO.setParentId(0L); // 按钮的parentId应为0
        when(resourceMapper.selectIdByCode("test:new")).thenReturn(null);
        when(resourceMapper.insert(any(ResourceEntity.class))).thenReturn(1);

        // 执行：按钮类型不检查父级是否存在
        assertDoesNotThrow(() -> resourceService.createResource(createDTO));

        // 验证：没有调用 selectById 检查父级
        verify(resourceMapper, never()).selectById(any(Long.class));
        verify(resourceMapper, times(1)).insert(any(ResourceEntity.class));
    }

    // ==================== updateResource 测试 ====================

    @Test
    @DisplayName("updateResource: 成功更新资源")
    void updateResource_shouldSuccess() {
        // 准备
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceMapper.selectIdByCode("test:update")).thenReturn(1L); // 排除自身
        when(resourceMapper.updateById(any(ResourceEntity.class))).thenReturn(1);

        updateDTO.setResourceCode("test:update");

        // 执行
        assertDoesNotThrow(() -> resourceService.updateResource(1L, updateDTO));

        // 验证
        verify(resourceMapper, times(1)).updateById(any(ResourceEntity.class));
    }

    @Test
    @DisplayName("updateResource: 数据不存在时抛出异常")
    void updateResource_whenNotExists_shouldThrowException() {
        // 准备
        when(resourceMapper.selectById(999999L)).thenReturn(null);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.updateResource(999999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
        verify(resourceMapper, never()).updateById(any(ResourceEntity.class));
    }

    @Test
    @DisplayName("updateResource: 按钮类型不允许修改父级和类型")
    void updateResource_buttonType_shouldNotChangeParentAndType() {
        // 准备：原资源是按钮
        testEntity.setResourceType(3);
        testEntity.setParentId(101L);
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceMapper.selectIdByCode(any())).thenReturn(null); // 模拟编码唯一性校验通过
        when(resourceMapper.updateById(any(ResourceEntity.class))).thenReturn(1);

        // 修改请求
        updateDTO.setParentId(999L); // 试图修改父级
        updateDTO.setResourceType(1);  // 试图修改类型

        // 执行
        assertDoesNotThrow(() -> resourceService.updateResource(1L, updateDTO));

        // 验证：父级和类型被强制保持不变
        verify(resourceMapper, times(1)).updateById(argThat((ResourceEntity entity) -> {
            return entity.getParentId().equals(101L) && entity.getResourceType().equals(3);
        }));
    }

    // ==================== deleteResource 测试 ====================

    @Test
    @DisplayName("deleteResource: 存在子资源时抛出异常")
    void deleteResource_whenHasChildren_shouldThrowException() {
        // 准备
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceMapper.countByParentId(1L)).thenReturn(3L); // 有子资源

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.deleteResource(1L)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_HAS_CHILDREN.getCode(), exception.getCode());
        verify(resourceMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("deleteResource: 有角色关联时抛出异常")
    void deleteResource_whenHasRoleRelation_shouldThrowException() {
        // 准备
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceMapper.countByParentId(1L)).thenReturn(0L);
        when(roleResourceMapper.countByResourceId(1L)).thenReturn(1L); // 有角色关联

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.deleteResource(1L)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_HAS_ROLES.getCode(), exception.getCode());
        verify(resourceMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("deleteResource: 数据不存在时抛出异常")
    void deleteResource_whenNotExists_shouldThrowException() {
        // 准备
        when(resourceMapper.selectById(999999L)).thenReturn(null);

        // 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> resourceService.deleteResource(999999L)
        );
        assertEquals(ErrorCodeEnum.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
        verify(resourceMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("deleteResource: 成功删除资源")
    void deleteResource_shouldSuccess() {
        // 准备
        when(resourceMapper.selectById(1L)).thenReturn(testEntity);
        when(resourceMapper.countByParentId(1L)).thenReturn(0L);
        when(roleResourceMapper.countByResourceId(1L)).thenReturn(0L);
        when(resourceMapper.deleteById(1L)).thenReturn(1);

        // 执行
        assertDoesNotThrow(() -> resourceService.deleteResource(1L));

        // 验证
        verify(resourceMapper, times(1)).deleteById(1L);
    }

    // ==================== pageResources with queryScope 测试 ====================

    @Test
    @DisplayName("pageResources: queryScope=2只查菜单")
    void pageResources_queryScopeMenu_shouldReturnMenusOnly() {
        // 准备
        Page<ResourceEntity> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of());

        when(resourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        com.yigongbao.module.system.resource.dto.ResourcePageDTO dto =
                new com.yigongbao.module.system.resource.dto.ResourcePageDTO();
        dto.setQueryScope(2);
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 执行
        var result = resourceService.pageResources(dto);

        // 断言
        assertNotNull(result);
        verify(resourceMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("pageResources: queryScope=3只查按钮")
    void pageResources_queryScopeButton_shouldReturnButtonsOnly() {
        // 准备
        Page<ResourceEntity> page = new Page<>(1, 10);
        page.setTotal(5);
        page.setRecords(List.of());

        when(resourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        com.yigongbao.module.system.resource.dto.ResourcePageDTO dto =
                new com.yigongbao.module.system.resource.dto.ResourcePageDTO();
        dto.setQueryScope(3);
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 执行
        var result = resourceService.pageResources(dto);

        // 断言
        assertNotNull(result);
        verify(resourceMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("pageResources: parentId筛选")
    void pageResources_withParentId_shouldFilterByParentId() {
        // 准备
        Page<ResourceEntity> page = new Page<>(1, 10);
        page.setTotal(3);
        page.setRecords(List.of());

        when(resourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        com.yigongbao.module.system.resource.dto.ResourcePageDTO dto =
                new com.yigongbao.module.system.resource.dto.ResourcePageDTO();
        dto.setParentId(1L);
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 执行
        var result = resourceService.pageResources(dto);

        // 断言
        assertNotNull(result);
        verify(resourceMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("pageResources: 默认queryScope=1返回全部类型")
    void pageResources_defaultQueryScope_shouldReturnAllTypes() {
        // 准备
        Page<ResourceEntity> page = new Page<>(1, 10);
        page.setTotal(3);
        page.setRecords(List.of());

        when(resourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 默认 dto，queryScope=null，resourceType=null，应返回全部类型混合数据
        com.yigongbao.module.system.resource.dto.ResourcePageDTO dto =
                new com.yigongbao.module.system.resource.dto.ResourcePageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 执行
        var result = resourceService.pageResources(dto);

        // 断言
        assertNotNull(result);
        verify(resourceMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== getResourceTree 测试 ====================

    @Test
    @DisplayName("getResourceTree: 返回资源树结构")
    void getResourceTree_shouldReturnTreeStructure() {
        // 准备
        ResourceEntity menu1 = new ResourceEntity();
        menu1.setId(1L);
        menu1.setParentId(0L);
        menu1.setResourceName("系统管理");
        menu1.setResourceCode("system");
        menu1.setResourceType(1);
        menu1.setSort(100);
        menu1.setVisible(1);
        menu1.setStatus(1);

        ResourceEntity menu2 = new ResourceEntity();
        menu2.setId(101L);
        menu2.setParentId(1L);
        menu2.setResourceName("机构管理");
        menu2.setResourceCode("system:org");
        menu2.setResourceType(2);
        menu2.setSort(1);
        menu2.setVisible(1);
        menu2.setStatus(1);

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(menu1, menu2));

        // 执行
        List<ResourceVO> result = resourceService.getResourceTree();

        // 断言
        assertNotNull(result);
        assertEquals(1, result.size()); // 只有根节点
        assertEquals("系统管理", result.get(0).getResourceName());
        assertNotNull(result.get(0).getChildren());
        assertEquals(1, result.get(0).getChildren().size()); // 有一个子节点
    }

    @Test
    @DisplayName("getResourceTree: 空数据返回空列表")
    void getResourceTree_whenEmpty_shouldReturnEmptyList() {
        // 准备
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        // 执行
        List<ResourceVO> result = resourceService.getResourceTree();

        // 断言
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getResourceTree: visible=0的资源被过滤不展示")
    void getResourceTree_withHiddenResources_shouldFilterByVisible() {
        // 准备：返回包含可见和隐藏资源的混合数据
        ResourceEntity visibleMenu = new ResourceEntity();
        visibleMenu.setId(1L);
        visibleMenu.setParentId(0L);
        visibleMenu.setResourceName("系统管理");
        visibleMenu.setResourceCode("system");
        visibleMenu.setResourceType(1);
        visibleMenu.setSort(100);
        visibleMenu.setVisible(1); // 可见
        visibleMenu.setStatus(1);

        ResourceEntity hiddenMenu = new ResourceEntity();
        hiddenMenu.setId(2L);
        hiddenMenu.setParentId(0L);
        hiddenMenu.setResourceName("隐藏菜单");
        hiddenMenu.setResourceCode("system:hidden");
        hiddenMenu.setResourceType(1);
        hiddenMenu.setSort(99);
        hiddenMenu.setVisible(0); // 隐藏
        hiddenMenu.setStatus(1);

        ResourceEntity disabledMenu = new ResourceEntity();
        disabledMenu.setId(3L);
        disabledMenu.setParentId(0L);
        disabledMenu.setResourceName("已禁用菜单");
        disabledMenu.setResourceCode("system:disabled");
        disabledMenu.setResourceType(1);
        disabledMenu.setSort(98);
        disabledMenu.setVisible(1);
        disabledMenu.setStatus(0); // 禁用

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(visibleMenu, hiddenMenu, disabledMenu));

        // 执行
        List<ResourceVO> result = resourceService.getResourceTree();

        // 断言：只返回 visible=1 且 status=1 的资源
        assertNotNull(result);
        assertEquals(1, result.size()); // 只剩一个可见资源
        assertEquals("系统管理", result.get(0).getResourceName());
        assertEquals(0, result.get(0).getChildren().size()); // 无子节点
    }

    // ==================== assignResources 测试 ====================

    @Test
    @DisplayName("assignResources: 正常分配资源")
    void assignResources_shouldSuccess() {
        // 准备
        List<Long> resourceIds = List.of(101L, 102L);

        // 模拟校验：两个资源ID都有效
        ResourceEntity res1 = new ResourceEntity();
        res1.setId(101L);
        res1.setStatus(1);
        ResourceEntity res2 = new ResourceEntity();
        res2.setId(102L);
        res2.setStatus(1);
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(res1, res2));

        // 执行
        assertDoesNotThrow(() -> resourceService.assignResources(2L, resourceIds));

        // 验证
        verify(roleResourceMapper, times(1)).deleteByRoleId(2L);
        verify(roleResourceMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("assignResources: 传入包含无效ID时过滤后分配")
    void assignResources_shouldFilterInvalidIds() {
        // 准备：传入两个ID，但只有一个有效
        List<Long> resourceIds = List.of(101L, 999L);

        ResourceEntity validRes = new ResourceEntity();
        validRes.setId(101L);
        validRes.setStatus(1);
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(validRes));

        // 执行
        assertDoesNotThrow(() -> resourceService.assignResources(2L, resourceIds));

        // 验证
        verify(roleResourceMapper, times(1)).deleteByRoleId(2L);
        verify(roleResourceMapper, times(1)).insertBatch(anyList());
    }

    @Test
    @DisplayName("assignResources: 清空角色资源")
    void assignResources_whenEmptyList_shouldClearResources() {
        // 准备：传入空列表
        List<Long> resourceIds = List.of();

        // 执行
        assertDoesNotThrow(() -> resourceService.assignResources(2L, resourceIds));

        // 验证：只删除了原有关联，没有插入
        verify(roleResourceMapper, times(1)).deleteByRoleId(2L);
        verify(roleResourceMapper, never()).insertBatch(anyList());
    }

    @Test
    @DisplayName("assignResources: null时清空资源")
    void assignResources_whenNull_shouldClearResources() {
        // 执行
        assertDoesNotThrow(() -> resourceService.assignResources(2L, null));

        // 验证
        verify(roleResourceMapper, times(1)).deleteByRoleId(2L);
        verify(roleResourceMapper, never()).insertBatch(anyList());
    }

    @Test
    @DisplayName("assignResources: 全部ID无效时不插入")
    void assignResources_whenAllIdsInvalid_shouldNotInsert() {
        // 准备：传入两个ID但都无效
        List<Long> resourceIds = List.of(998L, 999L);
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        // 执行
        assertDoesNotThrow(() -> resourceService.assignResources(2L, resourceIds));

        // 验证：删除后没有插入
        verify(roleResourceMapper, times(1)).deleteByRoleId(2L);
        verify(roleResourceMapper, never()).insertBatch(anyList());
    }

    // ==================== getResourceIdsByRoleId 测试 ====================

    @Test
    @DisplayName("getResourceIdsByRoleId: 返回角色关联的资源ID列表")
    void getResourceIdsByRoleId_shouldReturnResourceIds() {
        // 准备
        when(roleResourceMapper.selectResourceIdsByRoleId(1L))
                .thenReturn(List.of(1L, 101L, 102L));

        // 执行
        List<Long> result = resourceService.getResourceIdsByRoleId(1L);

        // 断言
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(101L));
    }

    @Test
    @DisplayName("getResourceIdsByRoleId: 无关联返回空列表")
    void getResourceIdsByRoleId_whenNoRelation_shouldReturnEmptyList() {
        // 准备
        when(roleResourceMapper.selectResourceIdsByRoleId(99L))
                .thenReturn(new ArrayList<>());

        // 执行
        List<Long> result = resourceService.getResourceIdsByRoleId(99L);

        // 断言
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getUserMenuTree 测试 ====================

    @Test
    @DisplayName("getUserMenuTree: 用户存在且有角色时返回菜单树")
    void getUserMenuTree_whenUserExists_shouldReturnMenuTree() {
        // 准备
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRoleId(1L);
        user.setUsername("admin");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleResourceMapper.selectResourceIdsByRoleId(1L))
                .thenReturn(List.of(1L, 101L));

        ResourceEntity menu1 = new ResourceEntity();
        menu1.setId(1L);
        menu1.setParentId(0L);
        menu1.setResourceName("系统管理");
        menu1.setResourceCode("system");
        menu1.setResourceType(1);
        menu1.setSort(100);
        menu1.setVisible(1);
        menu1.setStatus(1);

        ResourceEntity menu2 = new ResourceEntity();
        menu2.setId(101L);
        menu2.setParentId(1L);
        menu2.setResourceName("机构管理");
        menu2.setResourceCode("system:org");
        menu2.setResourceType(2);
        menu2.setSort(1);
        menu2.setVisible(1);
        menu2.setStatus(1);

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(menu1, menu2));

        // 执行
        List<ResourceVO> result = resourceService.getUserMenuTree(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userMapper, times(1)).selectById(1L);
        verify(roleResourceMapper, times(1)).selectResourceIdsByRoleId(1L);
    }

    @Test
    @DisplayName("getUserMenuTree: 用户不存在时返回空列表")
    void getUserMenuTree_whenUserNotExists_shouldReturnEmptyList() {
        // 准备
        when(userMapper.selectById(999999L)).thenReturn(null);

        // 执行
        List<ResourceVO> result = resourceService.getUserMenuTree(999999L);

        // 断言
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(roleResourceMapper, never()).selectResourceIdsByRoleId(any(Long.class));
    }

    @Test
    @DisplayName("getUserMenuTree: 用户无角色时返回空列表")
    void getUserMenuTree_whenUserHasNoRole_shouldReturnEmptyList() {
        // 准备
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRoleId(null); // 无角色

        when(userMapper.selectById(1L)).thenReturn(user);

        // 执行
        List<ResourceVO> result = resourceService.getUserMenuTree(1L);

        // 断言
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(roleResourceMapper, never()).selectResourceIdsByRoleId(any(Long.class));
    }

    // ==================== getUserPermissions 测试 ====================

    @Test
    @DisplayName("getUserPermissions: 用户存在时返回权限码列表")
    void getUserPermissions_whenUserExists_shouldReturnPermissions() {
        // 准备
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRoleId(1L);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleResourceMapper.selectResourceIdsByRoleId(1L))
                .thenReturn(List.of(1001L, 1002L));

        ResourceEntity btn1 = new ResourceEntity();
        btn1.setId(1001L);
        btn1.setResourceCode("system:org:add");
        btn1.setResourceType(3);
        btn1.setStatus(1);

        ResourceEntity btn2 = new ResourceEntity();
        btn2.setId(1002L);
        btn2.setResourceCode("system:org:edit");
        btn2.setResourceType(3);
        btn2.setStatus(1);

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(btn1, btn2));

        // 执行
        List<String> result = resourceService.getUserPermissions(1L);

        // 断言
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("system:org:add"));
        assertTrue(result.contains("system:org:edit"));
    }

    @Test
    @DisplayName("getUserPermissions: 用户不存在时返回空列表")
    void getUserPermissions_whenUserNotExists_shouldReturnEmptyList() {
        // 准备
        when(userMapper.selectById(999999L)).thenReturn(null);

        // 执行
        List<String> result = resourceService.getUserPermissions(999999L);

        // 断言
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getResourceTreeWithChecked 测试 ====================

    @Test
    @DisplayName("getResourceTreeWithChecked: 返回带checked状态的资源树")
    void getResourceTreeWithChecked_shouldReturnTreeWithChecked() {
        // 准备：两个资源，roleId=2L 关联了资源ID=101L
        ResourceEntity menu1 = new ResourceEntity();
        menu1.setId(1L);
        menu1.setParentId(0L);
        menu1.setResourceName("系统管理");
        menu1.setResourceCode("system");
        menu1.setResourceType(1);
        menu1.setSort(100);
        menu1.setVisible(1);
        menu1.setStatus(1);

        ResourceEntity button1 = new ResourceEntity();
        button1.setId(101L);
        button1.setParentId(1L);
        button1.setResourceName("新增机构");
        button1.setResourceCode("system:org:add");
        button1.setResourceType(3);
        button1.setSort(1);
        button1.setVisible(1);
        button1.setStatus(1);

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu1, button1));
        when(roleResourceMapper.selectResourceIdsByRoleId(2L)).thenReturn(List.of(101L));

        // 执行
        List<ResourceVO> result = resourceService.getResourceTreeWithChecked(2L);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.size()); // 只有一级菜单
        assertEquals("系统管理", result.get(0).getResourceName());
        assertFalse(result.get(0).getChecked()); // 一级菜单未勾选
        assertEquals(1, result.get(0).getChildren().size());
        assertTrue(result.get(0).getChildren().get(0).getChecked()); // 按钮已勾选
    }

    @Test
    @DisplayName("getResourceTreeWithChecked: roleId为null时全部checked=false")
    void getResourceTreeWithChecked_whenRoleIdNull_shouldReturnAllUnchecked() {
        // 准备
        ResourceEntity menu1 = new ResourceEntity();
        menu1.setId(1L);
        menu1.setParentId(0L);
        menu1.setResourceName("系统管理");
        menu1.setResourceCode("system");
        menu1.setResourceType(1);
        menu1.setSort(100);
        menu1.setVisible(1);
        menu1.setStatus(1);

        when(resourceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu1));

        // 执行
        List<ResourceVO> result = resourceService.getResourceTreeWithChecked(null);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getChecked());
        verify(roleResourceMapper, never()).selectResourceIdsByRoleId(any());
    }
}
