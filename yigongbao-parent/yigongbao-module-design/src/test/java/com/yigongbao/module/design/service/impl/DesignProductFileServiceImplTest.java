package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DesignProductFileServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignProductFileService 单元测试")
class DesignProductFileServiceImplTest {

    @Mock
    private DesignProductFileMapper productFileMapper;

    @InjectMocks
    private DesignProductFileServiceImpl productFileService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productFileService, productFileMapper);
    }

    @Test
    @DisplayName("listByProductId: 空结果时返回空列表")
    void listByProductId_noData_returnsEmpty() {
        when(productFileMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        List<DesignProductFileEntity> result = productFileService.listByProductId(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listByProductId: 返回对应记录")
    void listByProductId_withData_returnsFiles() {
        DesignProductFileEntity file = new DesignProductFileEntity();
        file.setDesignProductId(1L);
        file.setPackageFileId(100L);
        file.setPackageFileName("左髋骨.stl");
        when(productFileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(file));

        List<DesignProductFileEntity> result = productFileService.listByProductId(1L);
        assertEquals(1, result.size());
        assertEquals("左髋骨.stl", result.get(0).getPackageFileName());
    }

    @Test
    @DisplayName("listByProductIds: 空入参直接返回空列表，不查数据库")
    void listByProductIds_emptyInput_returnsEmpty() {
        List<DesignProductFileEntity> result = productFileService.listByProductIds(List.of());
        assertTrue(result.isEmpty());
        verify(productFileMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("removeByProductIds: 空入参直接跳过，不调用数据库")
    void removeByProductIds_emptyInput_skipsDb() {
        productFileService.removeByProductIds(List.of());
        verify(productFileMapper, never()).delete(any());
    }

    @Test
    @DisplayName("removeByProductId: 正常调用 remove")
    void removeByProductId_callsRemove() {
        when(productFileMapper.delete(any(Wrapper.class))).thenReturn(1);
        productFileService.removeByProductId(1L);
        verify(productFileMapper, times(1)).delete(any(Wrapper.class));
    }
}
