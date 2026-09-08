package com.yigongbao.module.production.product.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.flow.service.FlowStatusColorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private OrderMainService orderMainService;
    @Mock private UserMapper userMapper;
    @Mock private UserHospitalService userHospitalService;
    @Mock private FlowStatusColorResolver flowStatusColorResolver;

    @InjectMocks
    private ProductionProductServiceImpl productService;

    @BeforeEach
    void setUp() throws Exception {
        initTableInfo(ProductionRecordEntity.class);
        initTableInfo(ProductionProductEntity.class);
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

    @Test
    void pageProductDetails_mapsPrinterCodesFromBatchLoadedRecords() {
        ProductionRecordEntity firstRecord = record(10L, "printer-a");
        ProductionRecordEntity secondRecord = record(20L, "printer-b");
        IPage<ProductionProductDetailVO> result = pageProductDetails(
                List.of(p(1L, 10L, "P-001"), p(2L, 20L, "P-002")),
                List.of(firstRecord, secondRecord),
                List.of(firstRecord, secondRecord));

        assertEquals("printer-a", result.getRecords().get(0).getPrintDeviceCode());
        assertEquals("printer-b", result.getRecords().get(1).getPrintDeviceCode());
        verify(recordMapper, times(2)).selectList(any());
    }

    @Test
    void pageProductDetails_mapsPublicOrderCodeFromOrder() {
        ProductionRecordEntity record = record(10L, "printer-a");
        OrderMainEntity order = new OrderMainEntity();
        order.setId(100L);
        order.setPublicOrderCode("YGABC123456");
        record.setOrderId(100L);
        record.setOrderCode("ORD-001");
        when(orderMainService.listByIds(anyCollection())).thenReturn(List.of(order));

        IPage<ProductionProductDetailVO> result = pageProductDetails(
                List.of(p(1L, 10L, "P-001")), List.of(record), List.of(record));

        assertEquals("YGABC123456", result.getRecords().get(0).getPublicOrderCode());
    }

    @Test
    void pageProductDetails_keepsPrinterCodeNullWhenRecordCodeIsNull() {
        ProductionRecordEntity record = record(10L, null);
        IPage<ProductionProductDetailVO> result = pageProductDetails(
                List.of(p(1L, 10L, "P-001")), List.of(record), List.of(record));

        assertNull(result.getRecords().get(0).getPrintDeviceCode());
        verify(recordMapper, times(2)).selectList(any());
    }

    @Test
    void pageProductDetails_keepsPrinterCodeNullWhenBatchRecordIsMissing() {
        ProductionRecordEntity accessibleRecord = record(10L, "printer-a");
        IPage<ProductionProductDetailVO> result = pageProductDetails(
                List.of(p(1L, 10L, "P-001")), List.of(accessibleRecord), List.of());

        assertNull(result.getRecords().get(0).getPrintDeviceCode());
        verify(recordMapper, times(2)).selectList(any());
    }

    private IPage<ProductionProductDetailVO> pageProductDetails(
            List<ProductionProductEntity> products,
            List<ProductionRecordEntity> accessibleRecords,
            List<ProductionRecordEntity> hydratedRecords) {
        ProductionProductPageDTO dto = new ProductionProductPageDTO();
        UserEntity user = new UserEntity();
        Page<ProductionProductEntity> productPage = new Page<>(1, 10);
        productPage.setRecords(products);
        productPage.setTotal(products.size());

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
        when(recordMapper.selectList(any())).thenReturn(accessibleRecords, hydratedRecords);
        OrderMainEntity defaultOrder = new OrderMainEntity();
        defaultOrder.setId(100L);
        defaultOrder.setPublicOrderCode("YGABC123456");
        when(orderMainService.listByIds(anyCollection())).thenReturn(List.of(defaultOrder));
        when(productMapper.selectPage(any(Page.class), any())).thenReturn(productPage);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            return productService.pageProductDetails(dto);
        }
    }

    private ProductionRecordEntity record(Long id, String printDeviceCode) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(id);
        record.setPrintDeviceCode(printDeviceCode);
        return record;
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    private ProductionProductEntity p(Long id, String productNo) {
        return p(id, 10L, productNo);
    }

    private ProductionProductEntity p(Long id, Long recordId, String productNo) {
        ProductionProductEntity e = new ProductionProductEntity();
        e.setId(id);
        e.setProductionRecordId(recordId);
        e.setProductNo(productNo);
        return e;
    }
}
