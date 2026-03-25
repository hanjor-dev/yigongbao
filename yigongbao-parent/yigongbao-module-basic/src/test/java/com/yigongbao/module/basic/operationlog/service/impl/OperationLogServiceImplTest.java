package com.yigongbao.module.basic.operationlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.basic.operationlog.convert.OperationLogConvert;
import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.entity.OperationLogEntity;
import com.yigongbao.module.basic.operationlog.mapper.OperationLogMapper;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OperationLogServiceImpl Unit Test
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("OperationLogServiceImpl Unit Test")
class OperationLogServiceImplTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    private OperationLogServiceImpl operationLogService;

    private OperationLogEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        operationLogService = new OperationLogServiceImpl();

        Field baseMapperField = operationLogService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(operationLogService, operationLogMapper);

        testEntity = new OperationLogEntity();
        testEntity.setId(1L);
        testEntity.setModule("ModuleA");
        testEntity.setBusinessType(1);
        testEntity.setBusinessTypeName("Create");
        testEntity.setOperation("CreateUser");
        testEntity.setDescription("Test description");
        testEntity.setRequestMethod("POST");
        testEntity.setRequestUrl("/api/basic/user");
        testEntity.setIp("127.0.0.1");
        testEntity.setUserId(1L);
        testEntity.setUsername("admin");
        testEntity.setRealName("Administrator");
        testEntity.setStatus(1);
        testEntity.setDuration(100L);
        testEntity.setOperationTime(LocalDateTime.now());
    }

    // ==================== pageLogs test cases ====================

    @Test
    @DisplayName("pageLogs: status field should be correctly set")
    void pageLogs_shouldReturnCorrectStatus() {
        Page<OperationLogEntity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(testEntity));
        mockPage.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        OperationLogQueryDTO dto = new OperationLogQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        IPage<OperationLogVO> result = operationLogService.pageLogs(dto);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getRecords().get(0).getStatus());
    }

    @Test
    @DisplayName("pageLogs: disabled status should have value 0")
    void pageLogs_disabledStatus_shouldReturnZero() {
        testEntity.setStatus(0);
        Page<OperationLogEntity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(testEntity));
        mockPage.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        OperationLogQueryDTO dto = new OperationLogQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        IPage<OperationLogVO> result = operationLogService.pageLogs(dto);

        assertNotNull(result);
        assertEquals(0, result.getRecords().get(0).getStatus());
    }

    @Test
    @DisplayName("pageLogs: VO maps all fields")
    void pageLogs_shouldMapAllFields() {
        Page<OperationLogEntity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(testEntity));
        mockPage.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        OperationLogQueryDTO dto = new OperationLogQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        IPage<OperationLogVO> result = operationLogService.pageLogs(dto);
        OperationLogVO vo = result.getRecords().get(0);

        assertEquals(1L, vo.getId());
        assertEquals("ModuleA", vo.getModule());
        assertEquals("Create", vo.getBusinessTypeName());
        assertEquals("CreateUser", vo.getOperation());
        assertEquals("127.0.0.1", vo.getIp());
        assertEquals("admin", vo.getUsername());
        assertEquals("Administrator", vo.getRealName());
        assertEquals(100L, vo.getDuration());
        assertNotNull(vo.getOperationTime());
    }

    // ==================== list test cases ====================

    @Test
    @DisplayName("list: returns non-empty list")
    void list_shouldReturnNonEmptyList() {
        when(operationLogMapper.selectList(any()))
                .thenReturn(Collections.singletonList(testEntity));

        List<OperationLogEntity> list = operationLogService.list();

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("list: returns empty list")
    void list_shouldReturnEmptyList() {
        when(operationLogMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        List<OperationLogEntity> list = operationLogService.list();

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== OperationLogConvert test ====================

    @Test
    @DisplayName("OperationLogConvert.toVO: null entity returns null")
    void toVO_whenNull_shouldReturnNull() {
        assertNull(OperationLogConvert.toVO(null));
    }

    @Test
    @DisplayName("OperationLogConvert.toVO: maps all fields")
    void toVO_shouldMapAllFields() {
        OperationLogVO vo = OperationLogConvert.toVO(testEntity);

        assertNotNull(vo);
        assertEquals(testEntity.getId(), vo.getId());
        assertEquals(testEntity.getModule(), vo.getModule());
        assertEquals(testEntity.getBusinessTypeName(), vo.getBusinessTypeName());
        assertEquals(testEntity.getOperation(), vo.getOperation());
        assertEquals(testEntity.getRequestMethod(), vo.getRequestMethod());
        assertEquals(testEntity.getRequestUrl(), vo.getRequestUrl());
        assertEquals(testEntity.getIp(), vo.getIp());
        assertEquals(testEntity.getRealName(), vo.getRealName());
    }
}
