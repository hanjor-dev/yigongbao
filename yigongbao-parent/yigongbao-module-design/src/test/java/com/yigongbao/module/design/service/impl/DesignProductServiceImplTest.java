package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DesignProductServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-05-09
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignProductServiceImplTest {

    @Mock
    private DesignProductMapper designProductMapper;

    @InjectMocks
    private DesignProductServiceImpl designProductService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(designProductService, designProductMapper);
    }

    @Test
    void countByPackageId_success() {
        // Given
        Long packageId = 1L;
        when(designProductMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        // When
        long result = designProductService.countByPackageId(packageId);

        // Then
        assertEquals(3L, result);
        verify(designProductMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void countByPackageId_noProducts() {
        // Given
        Long packageId = 999L;
        when(designProductMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        long result = designProductService.countByPackageId(packageId);

        // Then
        assertEquals(0L, result);
        verify(designProductMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }
}
