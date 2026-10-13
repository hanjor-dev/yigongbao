package com.yigongbao.module.system.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.test.convert.TestConvert;
import com.yigongbao.module.system.test.dto.CreateTestDTO;
import com.yigongbao.module.system.test.dto.UpdateTestDTO;
import com.yigongbao.module.system.test.entity.TestEntity;
import com.yigongbao.module.system.test.mapper.TestMapper;
import com.yigongbao.module.system.test.service.TestService;
import com.yigongbao.module.system.test.vo.TestVO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TestService 单元测试
 *
 * @author hanjor
 * @date 2026-03-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("TestService 单元测试")
class TestServiceImplTest {

    @Mock
    private TestMapper testMapper;

    @InjectMocks
    private TestServiceImpl testService;

    private TestEntity testEntity;
    private CreateTestDTO createDTO;
    private UpdateTestDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 testMapper 注入到 ServiceImpl 的 baseMapper 字段中
        // 这是解决 MyBatis-Plus ServiceImpl 继承类单元测试的关键步骤
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(testService, testMapper);

        // 初始化测试数据
        LocalDateTime now = LocalDateTime.now();
        testEntity = new TestEntity();
        testEntity.setId(1L);
        testEntity.setKey1("test_key");
        testEntity.setValue1("test_value");
        testEntity.setCreateTime(now);
        testEntity.setUpdateTime(now);

        createDTO = new CreateTestDTO();
        createDTO.setKey("new_key");
        createDTO.setValue("new_value");

        updateDTO = new UpdateTestDTO();
        updateDTO.setKey("updated_key");
        updateDTO.setValue("updated_value");
    }

    // ==================== listVo 测试 ====================

    /**
     * 测试：查询所有数据 - 成功
     */
    @Test
    @DisplayName("listVo: 查询所有数据成功")
    void listVo_shouldReturnAllData() {
        // 准备
        List<TestEntity> entities = Arrays.asList(testEntity);
        when(testMapper.selectList(any())).thenReturn(entities);

        // 执行
        List<TestVO> result = testService.listVo();

        // 断言
        assertNotNull(result, "结果不应为空");
        assertEquals(1, result.size(), "应返回1条数据");
        assertEquals("test_key", result.get(0).getKey1(), "key应匹配");
        assertEquals("test_value", result.get(0).getValue1(), "value应匹配");
        verify(testMapper, times(1)).selectList(any());
    }

    /**
     * 测试：查询所有数据 - 返回空列表
     */
    @Test
    @DisplayName("listVo: 无数据时返回空列表")
    void listVo_whenNoData_shouldReturnEmptyList() {
        // 准备
        when(testMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 执行
        List<TestVO> result = testService.listVo();

        // 断言
        assertNotNull(result, "结果不应为空");
        assertTrue(result.isEmpty(), "应返回空列表");
        verify(testMapper, times(1)).selectList(any());
    }

    // ==================== getVoById 测试 ====================

    /**
     * 测试：根据ID查询 - 成功
     */
    @Test
    @DisplayName("getVoById: 存在数据时返回VO")
    void getVoById_whenExists_shouldReturnData() {
        // 准备
        when(testMapper.selectById(1L)).thenReturn(testEntity);

        // 执行
        TestVO result = testService.getVoById(1L);

        // 断言
        assertNotNull(result, "结果不应为空");
        assertEquals(1L, result.getId(), "ID应匹配");
        assertEquals("test_key", result.getKey1(), "key应匹配");
        assertEquals("test_value", result.getValue1(), "value应匹配");
        verify(testMapper, times(1)).selectById(1L);
    }

    /**
     * 测试：根据ID查询 - 数据不存在
     */
    @Test
    @DisplayName("getVoById: 数据不存在时抛出异常")
    void getVoById_whenNotExists_shouldThrowException() {
        // 准备
        when(testMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> testService.getVoById(999L),
            "应抛出BusinessException"
        );

        // 验证错误码
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode(), "错误码应为404");
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getMessage(), exception.getMessage(), "错误信息应匹配");
        verify(testMapper, times(1)).selectById(999L);
    }

    // ==================== create 测试 ====================

    /**
     * 测试：创建数据 - 成功
     */
    @Test
    @DisplayName("create: 创建数据成功")
    void create_shouldSuccess() {
        // 准备
        when(testMapper.insert(any(TestEntity.class))).thenReturn(1);

        // 执行
        testService.create(createDTO);

        // 断言：验证 insert 被调用
        verify(testMapper, times(1)).insert(any(TestEntity.class));
    }

    // ==================== update 测试 ====================

    /**
     * 测试：更新数据 - 成功
     */
    @Test
    @DisplayName("update: 更新数据成功")
    void update_whenExists_shouldSuccess() {
        // 准备
        when(testMapper.selectById(1L)).thenReturn(testEntity);
        when(testMapper.updateById(any(TestEntity.class))).thenReturn(1);

        // 执行
        testService.update(1L, updateDTO);

        // 断言
        verify(testMapper, times(1)).selectById(1L);
        verify(testMapper, times(1)).updateById(any(TestEntity.class));
    }

    /**
     * 测试：更新数据 - 数据不存在
     */
    @Test
    @DisplayName("update: 数据不存在时抛出异常")
    void update_whenNotExists_shouldThrowException() {
        // 准备
        when(testMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> testService.update(999L, updateDTO),
            "应抛出BusinessException"
        );

        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode(), "错误码应为404");
        verify(testMapper, times(1)).selectById(999L);
        verify(testMapper, never()).updateById(any(TestEntity.class));
    }

    // ==================== remove 测试 ====================

    /**
     * 测试：删除数据 - 成功
     */
    @Test
    @DisplayName("remove: 删除数据成功")
    void remove_whenExists_shouldSuccess() {
        // 准备：removeById 内部调用 deleteById，返回 boolean
        when(testMapper.deleteById(1L)).thenReturn(1);

        // 执行
        testService.remove(1L);

        // 断言
        verify(testMapper, times(1)).deleteById(1L);
    }

    /**
     * 测试：删除数据 - 数据不存在
     */
    @Test
    @DisplayName("remove: 数据不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        // 准备：deleteById 返回 0 表示未删除任何数据
        when(testMapper.deleteById(999L)).thenReturn(0);

        // 执行 & 断言
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> testService.remove(999L),
            "应抛出BusinessException"
        );

        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode(), "错误码应为404");
        verify(testMapper, times(1)).deleteById(999L);
    }
}
