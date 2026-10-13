package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderItemServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-05-09
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（继承 ServiceImpl 时必须）
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orderItemService, orderItemMapper);
    }

    private OrderItemEntity buildMockItem(Long id, Long orderId) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(id);
        entity.setOrderId(orderId);
        return entity;
    }

    @Test
    void listByOrderId_success() {
        // Given
        Long orderId = 1L;
        List<OrderItemEntity> mockItems = Arrays.asList(
            buildMockItem(1L, orderId),
            buildMockItem(2L, orderId)
        );
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockItems);

        // When
        List<OrderItemEntity> result = orderItemService.listByOrderId(orderId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderItemMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listByOrderIds_success() {
        // Given
        List<Long> orderIds = Arrays.asList(1L, 2L);
        List<OrderItemEntity> mockItems = Arrays.asList(
            buildMockItem(1L, 1L),
            buildMockItem(2L, 1L),
            buildMockItem(3L, 2L)
        );
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockItems);

        // When
        List<OrderItemEntity> result = orderItemService.listByOrderIds(orderIds);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(orderItemMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listByOrderIds_emptyInput() {
        // Given
        List<Long> orderIds = Collections.emptyList();

        // When
        List<OrderItemEntity> result = orderItemService.listByOrderIds(orderIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderItemMapper, never()).selectList(any());
    }

    @Test
    void listByOrderIds_nullInput() {
        // When
        List<OrderItemEntity> result = orderItemService.listByOrderIds(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderItemMapper, never()).selectList(any());
    }
}
