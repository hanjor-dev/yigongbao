package com.yigongbao.module.order.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.entity.OrderMainEntity;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("OrderMainService 单元测试")
class OrderMainServiceImplTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderMainServiceImpl orderMainService;

    private OrderMainEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orderMainService, orderMainMapper);

        testEntity = new OrderMainEntity();
        testEntity.setId(1L);
        testEntity.setOrderCode("ORDER-001");
    }
}
