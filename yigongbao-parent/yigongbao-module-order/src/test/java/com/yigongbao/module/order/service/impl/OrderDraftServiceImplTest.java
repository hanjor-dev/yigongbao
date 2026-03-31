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

import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("OrderDraftService 单元测试")
class OrderDraftServiceImplTest {

    @Mock
    private OrderDraftMapper orderDraftMapper;

    @Mock
    private OrderItemDraftMapper orderItemDraftMapper;

    @InjectMocks
    private OrderDraftServiceImpl orderDraftService;

    private OrderDraftEntity testDraft;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 orderDraftMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orderDraftService, orderDraftMapper);

        testDraft = new OrderDraftEntity();
        testDraft.setId(1L);
        testDraft.setOperatorId(1L);
    }
}
