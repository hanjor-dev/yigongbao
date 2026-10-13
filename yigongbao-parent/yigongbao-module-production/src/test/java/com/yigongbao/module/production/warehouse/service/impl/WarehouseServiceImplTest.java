package com.yigongbao.module.production.warehouse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseDTO;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseProductDTO;
import com.yigongbao.module.production.warehouse.vo.WarehouseRecordVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseProductVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarehouseServiceImplTest {

    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void listWarehouse_normalizesDateEndToNextDayExclusiveBoundary() {
        ListWarehouseDTO dto = new ListWarehouseDTO();
        dto.setPage(1);
        dto.setSize(10);
        dto.setWarehouseInTimeEnd(LocalDateTime.of(2026, 8, 25, 0, 0));
        when(recordMapper.listWarehouse(any(Page.class), any(ListWarehouseDTO.class)))
                .thenReturn(new Page<WarehouseRecordVO>(1, 10));

        warehouseService.listWarehouse(dto);

        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0), dto.getWarehouseInTimeEnd());
    }

    @Test
    void listWarehouseProducts_normalizesDateEndsToNextDayExclusiveBoundary() {
        ListWarehouseProductDTO dto = new ListWarehouseProductDTO();
        dto.setPage(1);
        dto.setSize(10);
        dto.setWarehouseInTimeEnd(LocalDateTime.of(2026, 8, 25, 0, 0));
        dto.setWarehouseOutTimeEnd(LocalDateTime.of(2026, 8, 25, 0, 0));
        when(recordMapper.listWarehouseProducts(any(Page.class), any(ListWarehouseProductDTO.class)))
                .thenReturn(new Page<WarehouseProductVO>(1, 10));

        warehouseService.listWarehouseProducts(dto);

        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0), dto.getWarehouseInTimeEnd());
        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0), dto.getWarehouseOutTimeEnd());
    }

    // ==================== warehouseInProduct ====================

    @Test
    void warehouseInProduct_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> warehouseService.warehouseInProduct(99L, new WarehouseInProductDTO()));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void warehouseInProduct_wrongStatus_throwsException() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSED));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO()));
        assertEquals(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_IN.getCode(), ex.getCode());
    }

    @Test
    void warehouseInProduct_cancelledStatus_throwsException() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.CANCELLED));
        assertThrows(BusinessException.class, () -> warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO()));
    }

    @Test
    void warehouseInProduct_success_updatesProductFields() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.PENDING_WAREHOUSE_IN));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO());
        }

        verify(productMapper).updateById(argThat((ProductionProductEntity obj) -> {
            ProductionProductEntity e = (ProductionProductEntity) obj;
            return ProductStatusEnum.WAREHOUSED.getCode().equals(e.getStatus())
                && e.getWarehouseInTime() != null
                && Long.valueOf(1L).equals(e.getWarehouseInUserId());
        }));
        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
    }

    @Test
    void warehouseInProduct_allProductsWarehoused_updatesRecordAndTriggersFlow() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.PENDING_WAREHOUSE_IN));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 3L); // pending=0, warehoused=3
        when(recordMapper.selectById(10L)).thenReturn(record(10L, FlowStatusEnum.PENDING_WAREHOUSE_IN, 100L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO());
        }

        verify(recordMapper).updateById(argThat((ProductionRecordEntity obj) ->
            FlowStatusEnum.WAREHOUSED.getValue().equals(((ProductionRecordEntity) obj).getStatus())));
        verify(recordService).triggerFlowIfAllReach(
            100L, FlowStatusEnum.WAREHOUSED.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_IN);
        verify(recordService).reconcileOrderProductionStatus(100L);
    }

    @Test
    void warehouseInProduct_noWarehousedProducts_skipsRecordUpdate() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.PENDING_WAREHOUSE_IN));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 0L); // pending=0, warehoused=0

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO());
        }

        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
        verify(recordService, never()).triggerFlowIfAllExact(any(), any(), any());
    }

    @Test
    void warehouseInProduct_recordAlreadyWarehoused_concurrentSkip() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.PENDING_WAREHOUSE_IN));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 2L);
        // 流转卡已推进（并发场景）
        when(recordMapper.selectById(10L)).thenReturn(record(10L, FlowStatusEnum.WAREHOUSED, 100L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseInProduct(1L, new WarehouseInProductDTO());
        }

        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
    }

    // ==================== warehouseOutProduct ====================

    @Test
    void warehouseOutProduct_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> warehouseService.warehouseOutProduct(99L, new WarehouseOutProductDTO()));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void warehouseOutProduct_wrongStatus_pendingIn_throwsException() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.PENDING_WAREHOUSE_IN));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO()));
        assertEquals(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_OUT.getCode(), ex.getCode());
    }

    @Test
    void warehouseOutProduct_wrongStatus_alreadyOut_throwsException() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSE_OUT));
        BusinessException ex = assertThrows(BusinessException.class,
            () -> warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO()));
        assertEquals(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_OUT.getCode(), ex.getCode());
    }

    @Test
    void warehouseOutProduct_success_updatesProductFields() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSED));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO());
        }

        verify(productMapper).updateById(argThat((ProductionProductEntity obj) -> {
            ProductionProductEntity e = (ProductionProductEntity) obj;
            return ProductStatusEnum.WAREHOUSE_OUT.getCode().equals(e.getStatus())
                && e.getWarehouseOutTime() != null
                && Long.valueOf(1L).equals(e.getWarehouseOutUserId());
        }));
        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
    }

    @Test
    void warehouseOutProduct_allProductsOut_updatesRecordAndTriggersFlow() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSED));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 3L); // warehoused=0, out=3
        when(recordMapper.selectById(10L)).thenReturn(record(10L, FlowStatusEnum.WAREHOUSED, 100L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO());
        }

        verify(recordMapper).updateById(argThat((ProductionRecordEntity obj) ->
            FlowStatusEnum.WAREHOUSE_OUT.getValue().equals(((ProductionRecordEntity) obj).getStatus())));
        verify(recordService).triggerFlowIfAllReach(
            100L, FlowStatusEnum.WAREHOUSE_OUT.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_OUT);
        verify(recordService).reconcileOrderProductionStatus(100L);
    }

    @Test
    void warehouseOutProduct_partialOut_noFlowTrigger() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSED));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L); // 仍有WAREHOUSED

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO());
        }

        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
        verify(recordService, never()).triggerFlowIfAllExact(any(), any(), any());
    }

    @Test
    void warehouseOutProduct_recordAlreadyOut_concurrentSkip() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L, ProductStatusEnum.WAREHOUSED));
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 1L);
        // 流转卡已是WAREHOUSE_OUT（并发场景）
        when(recordMapper.selectById(10L)).thenReturn(record(10L, FlowStatusEnum.WAREHOUSE_OUT, 100L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.warehouseOutProduct(1L, new WarehouseOutProductDTO());
        }

        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
    }

    // ==================== helpers ====================

    private ProductionProductEntity product(Long id, Long recordId, ProductStatusEnum status) {
        ProductionProductEntity p = new ProductionProductEntity();
        p.setId(id);
        p.setProductionRecordId(recordId);
        p.setStatus(status.getCode());
        p.setProductNo("PROD-" + id);
        return p;
    }

    private ProductionRecordEntity record(Long id, FlowStatusEnum status, Long orderId) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setStatus(status.getValue());
        r.setOrderId(orderId);
        r.setRecordNo("REC-" + id);
        return r;
    }
}
