package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionProductServiceImplTest {

    @Mock private ProductionProductMapper productMapper;

    @InjectMocks
    private ProductionProductServiceImpl productService;

    @BeforeEach
    void setUp() throws Exception {
        Field f = ServiceImpl.class.getDeclaredField("baseMapper");
        f.setAccessible(true);
        f.set(productService, productMapper);
    }

    @Test
    void listByRecordId_returnsOrderedList() {
        when(productMapper.selectList(any())).thenReturn(List.of(p(1L, "P-001"), p(2L, "P-002")));
        assertEquals(2, productService.listByRecordId(10L).size());
    }

    @Test
    void getByProductNo_notFound_throwsException() {
        when(productMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> productService.getByProductNo("P-999")).getCode());
    }

    @Test
    void getByProductNo_found_returnsEntity() {
        when(productMapper.selectOne(any())).thenReturn(p(1L, "P-001"));
        when(productMapper.selectOne(any(), anyBoolean())).thenReturn(p(1L, "P-001"));
        assertEquals("P-001", productService.getByProductNo("P-001").getProductNo());
    }

    @Test
    void updateStatus_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> productService.updateStatus(99L, "pass")).getCode());
    }

    @Test
    void updateStatus_found_updatesStatus() {
        ProductionProductEntity product = p(1L, "P-001");
        product.setStatus("in_process");
        when(productMapper.selectById(1L)).thenReturn(product);

        productService.updateStatus(1L, "pass");

        verify(productMapper).updateById((ProductionProductEntity) argThat(e ->
                "pass".equals(((ProductionProductEntity) e).getStatus())));
    }

    private ProductionProductEntity p(Long id, String productNo) {
        ProductionProductEntity e = new ProductionProductEntity();
        e.setId(id);
        e.setProductionRecordId(10L);
        e.setProductNo(productNo);
        return e;
    }
}
