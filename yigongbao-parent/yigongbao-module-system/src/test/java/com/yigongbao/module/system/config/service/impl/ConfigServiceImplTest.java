package com.yigongbao.module.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.entity.ConfigEntity;
import com.yigongbao.module.system.config.mapper.ConfigMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.config.vo.ConfigVO;
import com.yigongbao.module.system.config.convert.ConfigConvert;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 配置 Service 单元测试
 *
 * @author hanjor
 * @date 2026-03-18
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConfigService 单元测试")
class ConfigServiceImplTest {

    @Mock
    private ConfigMapper configMapper;

    @Mock
    private ConfigConvert configConvert;

    @InjectMocks
    private ConfigServiceImpl configService;

    private ConfigEntity testEntity;
    private CreateConfigDTO createDTO;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 configMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(configService, configMapper);

        // 初始化测试数据
        testEntity = new ConfigEntity();
        testEntity.setId(1L);
        testEntity.setConfigKey("test.config");
        testEntity.setConfigName("测试配置");
        testEntity.setConfigValue("testValue");
        testEntity.setConfigType("string");
        testEntity.setConfigGroup("system");
        testEntity.setConfigDesc("测试描述");
        testEntity.setIsSystem(0);
        testEntity.setIsPublic(1);
        testEntity.setSort(1);
        testEntity.setStatus(1);

        createDTO = new CreateConfigDTO();
        createDTO.setConfigKey("new.config");
        createDTO.setConfigName("新配置");
        createDTO.setConfigValue("newValue");
        createDTO.setConfigType("string");
        createDTO.setConfigGroup("security");
    }

    // ==================== pageConfig 测试 ====================

    @Test
    @DisplayName("pageConfig: 分页查询成功")
    void pageConfig_shouldReturnPageData() {
        List<ConfigEntity> list = Arrays.asList(testEntity);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConfigEntity> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(list);
        page.setTotal(1);

        when(configMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        // Mock convert 转换
        ConfigVO mockVO = new ConfigVO();
        mockVO.setId(1L);
        mockVO.setConfigKey("test.config");
        when(configConvert.toVO(any(ConfigEntity.class))).thenReturn(mockVO);

        IPage<ConfigVO> result = configService.pageConfig(1, 10, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("test.config", result.getRecords().get(0).getConfigKey());
    }

    @Test
    @DisplayName("pageConfig: 无数据时返回空列表")
    void pageConfig_whenNoData_shouldReturnEmptyList() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConfigEntity> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(configMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<ConfigVO> result = configService.pageConfig(1, 10, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getConfigById 测试 ====================

    @Test
    @DisplayName("getConfigById: 存在数据时返回VO")
    void getConfigById_whenExists_shouldReturnData() {
        when(configMapper.selectById(1L)).thenReturn(testEntity);

        // Mock convert 转换
        ConfigVO mockVO = new ConfigVO();
        mockVO.setId(1L);
        mockVO.setConfigKey("test.config");
        when(configConvert.toVO(any(ConfigEntity.class))).thenReturn(mockVO);

        ConfigVO result = configService.getConfigById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test.config", result.getConfigKey());
    }

    @Test
    @DisplayName("getConfigById: 数据不存在时抛出异常")
    void getConfigById_whenNotExists_shouldThrowException() {
        when(configMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.getConfigById(999L)
        );
        assertEquals(ErrorCodeEnum.CONFIG_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== getConfigByKey 测试 ====================

    @Test
    @DisplayName("getConfigByKey: 存在数据时返回VO")
    void getConfigByKey_whenExists_shouldReturnData() {
        // getOne 方法调用的是 baseMapper.selectOne，需要 mock selectOne
        // 使用 any() 匹配任意参数，而不是 any(LambdaQueryWrapper.class)
        when(configMapper.selectOne(any())).thenReturn(testEntity);

        // Mock convert 转换
        ConfigVO mockVO = new ConfigVO();
        mockVO.setConfigKey("test.config");
        when(configConvert.toVO(any(ConfigEntity.class))).thenReturn(mockVO);

        ConfigVO result = configService.getConfigByKey("test.config");

        assertNotNull(result);
        assertEquals("test.config", result.getConfigKey());
    }

    @Test
    @DisplayName("getConfigByKey: 数据不存在时抛出异常")
    void getConfigByKey_whenNotExists_shouldThrowException() {
        // getOne 方法调用的是 baseMapper.selectOne，需要 mock selectOne 返回 null
        when(configMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.getConfigByKey("not_exists")
        );
        assertEquals(ErrorCodeEnum.CONFIG_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createConfig 测试 ====================

    @Test
    @DisplayName("createConfig: 创建成功")
    void createConfig_shouldSuccess() {
        // 需要 mock count 方法返回 0，模拟配置键不存在（isConfigKeyExists 调用的是 count）
        when(configMapper.selectCount(any())).thenReturn(0L);
        when(configMapper.insert(any(ConfigEntity.class))).thenReturn(1);
        // Mock convert 转换
        ConfigEntity mockEntity = new ConfigEntity();
        mockEntity.setId(1L);
        when(configConvert.toEntity(any(CreateConfigDTO.class))).thenReturn(mockEntity);

        assertDoesNotThrow(() -> configService.createConfig(createDTO));
        verify(configMapper, times(1)).insert(any(ConfigEntity.class));
    }

    @Test
    @DisplayName("createConfig: configKey重复时抛出异常")
    void createConfig_whenKeyExists_shouldThrowException() {
        // 需要 mock count 方法返回 > 0，模拟配置键已存在（isConfigKeyExists 调用的是 count）
        when(configMapper.selectCount(any())).thenReturn(1L);
        // 也需要 mock toEntity，因为方法会先调用 toEntity 再检查重复
        ConfigEntity mockEntity = new ConfigEntity();
        mockEntity.setId(1L);
        when(configConvert.toEntity(any(CreateConfigDTO.class))).thenReturn(mockEntity);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.createConfig(createDTO)
        );
        assertEquals(ErrorCodeEnum.CONFIG_KEY_EXISTS.getCode(), exception.getCode());
    }

    // ==================== updateConfig 测试 ====================

    @Test
    @DisplayName("updateConfig: 更新成功")
    void updateConfig_shouldSuccess() {
        when(configMapper.selectById(1L)).thenReturn(testEntity);
        when(configMapper.updateById(any(ConfigEntity.class))).thenReturn(1);
        // Mock convert 转换（updateEntity 是 void 方法，不需要返回值）
        doNothing().when(configConvert).updateEntity(any(UpdateConfigDTO.class), any(ConfigEntity.class));

        UpdateConfigDTO dto = new UpdateConfigDTO();
        dto.setConfigName("更新后的名称");

        assertDoesNotThrow(() -> configService.updateConfig(1L, dto));
        verify(configMapper, times(1)).updateById(any(ConfigEntity.class));
    }

    @Test
    @DisplayName("updateConfig: 数据不存在时抛出异常")
    void updateConfig_whenNotExists_shouldThrowException() {
        when(configMapper.selectById(999L)).thenReturn(null);

        UpdateConfigDTO dto = new UpdateConfigDTO();

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.updateConfig(999L, dto)
        );
        assertEquals(ErrorCodeEnum.CONFIG_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateConfig: 系统内置配置不可修改")
    void updateConfig_whenSystemConfig_shouldThrowException() {
        testEntity.setIsSystem(1);
        when(configMapper.selectById(1L)).thenReturn(testEntity);

        UpdateConfigDTO dto = new UpdateConfigDTO();
        dto.setConfigName("更新名称");

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.updateConfig(1L, dto)
        );
        assertEquals(ErrorCodeEnum.CONFIG_SYSTEM_NOT_ALLOW_UPDATE.getCode(), exception.getCode());
    }

    // ==================== deleteConfig 测试 ====================

    @Test
    @DisplayName("deleteConfig: 删除成功")
    void deleteConfig_shouldSuccess() {
        when(configMapper.selectById(1L)).thenReturn(testEntity);
        when(configMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> configService.deleteConfig(1L));
        verify(configMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteConfig: 数据不存在时抛出异常")
    void deleteConfig_whenNotExists_shouldThrowException() {
        when(configMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.deleteConfig(999L)
        );
        assertEquals(ErrorCodeEnum.CONFIG_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("deleteConfig: 系统内置配置不可删除")
    void deleteConfig_whenSystemConfig_shouldThrowException() {
        testEntity.setIsSystem(1);
        when(configMapper.selectById(1L)).thenReturn(testEntity);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> configService.deleteConfig(1L)
        );
        assertEquals(ErrorCodeEnum.CONFIG_SYSTEM_NOT_ALLOW_DELETE.getCode(), exception.getCode());
    }

    // ==================== listPublicConfig 测试 ====================

    @Test
    @DisplayName("listPublicConfig: 返回公开配置")
    void listPublicConfig_shouldReturnPublicConfigs() {
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testEntity));
        // Mock convert 转换
        ConfigVO mockVO = new ConfigVO();
        when(configConvert.toVO(any(ConfigEntity.class))).thenReturn(mockVO);

        List<ConfigVO> result = configService.listPublicConfig();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listPublicConfig: 无公开配置时返回空列表")
    void listPublicConfig_whenNoData_shouldReturnEmptyList() {
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<ConfigVO> result = configService.listPublicConfig();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== listAllConfig 测试 ====================

    @Test
    @DisplayName("listAllConfig: 返回所有配置")
    void listAllConfig_shouldReturnAllConfigs() {
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testEntity));
        // Mock convert 转换
        ConfigVO mockVO = new ConfigVO();
        when(configConvert.toVO(any(ConfigEntity.class))).thenReturn(mockVO);

        List<ConfigVO> result = configService.listAllConfig();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listAllConfig: 无数据时返回空列表")
    void listAllConfig_whenNoData_shouldReturnEmptyList() {
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<ConfigVO> result = configService.listAllConfig();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== listConfigGroups 测试 ====================

    @Test
    @DisplayName("listConfigGroups: 返回预设分组")
    void listConfigGroups_shouldReturnGroups() {
        List<com.yigongbao.module.system.basedata.vo.SelectTreeVO> result = configService.listConfigGroups();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("system", result.get(0).getValue());
        assertEquals("security", result.get(1).getValue());
        assertEquals("other", result.get(2).getValue());
    }
}
