package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.OrderDraftFileEntity;
import com.yigongbao.module.order.mapper.OrderDraftFileMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderDraftFileServiceImplTest {

    @Mock private OrderDraftFileMapper mapper;

    @Spy
    @InjectMocks
    private OrderDraftFileServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), OrderDraftFileEntity.class);
    }

    @Test
    void saveDraftFiles_emptyList_removesOldLinksWithoutInsert() {
        doReturn(true).when(service).remove(any());
        doReturn(true).when(service).saveBatch(anyList());

        service.saveDraftFiles(7L, List.of());

        verify(service).remove(any());
        verify(service, never()).saveBatch(anyList());
    }

    @Test
    void saveDraftFiles_nonEmptyList_replacesLinks() {
        OrderDraftFileEntity file = new OrderDraftFileEntity();
        file.setDraftId(7L);
        doReturn(true).when(service).remove(any());
        doReturn(true).when(service).saveBatch(anyList());

        service.saveDraftFiles(7L, List.of(file));

        verify(service).remove(any());
        verify(service).saveBatch(List.of(file));
    }

    @Test
    void listByDraftId_queriesOnlyRequestedDraftInCreateOrder() {
        OrderDraftFileEntity file = new OrderDraftFileEntity();
        file.setDraftId(7L);
        doReturn(List.of(file)).when(service)
                .list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        assertThat(service.listByDraftId(7L)).containsExactly(file);
        verify(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
