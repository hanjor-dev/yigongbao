package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import org.junit.jupiter.api.BeforeEach;
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
 * OrderFileServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-05-09
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderFileServiceImplTest {

    @Mock
    private OrderFileMapper orderFileMapper;

    @InjectMocks
    private OrderFileServiceImpl orderFileService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orderFileService, orderFileMapper);
    }

    private OrderFileEntity buildMockFile(Long id, Long orderId, String category) {
        OrderFileEntity entity = new OrderFileEntity();
        entity.setId(id);
        entity.setOrderId(orderId);
        entity.setFileCategory(category);
        return entity;
    }

    @Test
    void listByOrderIdAndCategory_success() {
        // Given
        Long orderId = 1L;
        String category = "DICOM";
        List<OrderFileEntity> mockFiles = Arrays.asList(
            buildMockFile(1L, orderId, category),
            buildMockFile(2L, orderId, category)
        );
        when(orderFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockFiles);

        // When
        List<OrderFileEntity> result = orderFileService.listByOrderIdAndCategory(orderId, category);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderFileMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }
}
