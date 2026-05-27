package com.yigongbao.module.production.qc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.qc.dto.ProductionRedoPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionQcServiceImplTest {

    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private ProductionQcServiceImpl qcService;

    // ---- markProductPass ----

    @Test
    void markProductPass_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> qcService.markProductPass(99L)).getCode());
    }

    @Test
    void markProductPass_recordNotFound_throwsException() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                    assertThrows(BusinessException.class, () -> qcService.markProductPass(1L)).getCode());
        }
    }

    @Test
    void markProductPass_nonMedical_noUdiGenerated() {
        ProductionRecordEntity rec = record(10L, ProductionConstants.ORDER_TYPE_NON_MEDICAL);
        rec.setQualifiedCount(0);
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(rec);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductPass(1L);
        }
        verify(codeGeneratorService, never()).generate(ProductionConstants.UDI_CODE);
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                ProductStatusEnum.PASS.getCode().equals(((ProductionProductEntity) p).getStatus())
                        && QcResultEnum.PASS.getCode().equals(((ProductionProductEntity) p).getQcResult())));
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                Integer.valueOf(1).equals(((ProductionRecordEntity) r).getQualifiedCount())));
    }

    @Test
    void markProductPass_medical_generatesUdi() {
        ProductionRecordEntity rec = record(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        rec.setQualifiedCount(0);
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(rec);
        when(codeGeneratorService.generate(ProductionConstants.UDI_CODE)).thenReturn("UDI-001");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductPass(1L);
        }
        verify(codeGeneratorService).generate(ProductionConstants.UDI_CODE);
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                "UDI-001".equals(((ProductionProductEntity) p).getUdiCode())));
    }

    // ---- markProductRedo ----

    @Test
    void markProductRedo_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                    assertThrows(BusinessException.class, () -> qcService.markProductRedo(99L, "r")).getCode());
        }
    }

    @Test
    void markProductRedo_recordNull_onlyUpdatesProduct() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductRedo(1L, "defect");
        }
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                ProductStatusEnum.REDO.getCode().equals(((ProductionProductEntity) p).getStatus())));
        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
    }

    @Test
    void markProductRedo_recordFound_updatesCountAndFlag() {
        ProductionRecordEntity rec = record(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        rec.setUnqualifiedCount(0);
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(rec);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductRedo(1L, "defect");
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r -> {
            ProductionRecordEntity e = (ProductionRecordEntity) r;
            return Integer.valueOf(1).equals(e.getUnqualifiedCount())
                    && Integer.valueOf(1).equals(e.getHasRedoProduct());
        }));
    }

    // ---- assignRedoProcess ----

    @Test
    void assignRedoProcess_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> qcService.assignRedoProcess(99L, "print")).getCode());
    }

    @Test
    void assignRedoProcess_found_setsRedoProcessType() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        qcService.assignRedoProcess(1L, "wash");
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                "wash".equals(((ProductionProductEntity) p).getRedoProcessType())));
    }

    // ---- transferToPacking ----

    @Test
    void transferToPacking_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> qcService.transferToPacking(99L)).getCode());
    }

    @Test
    void transferToPacking_notAllPass_throwsException() {
        when(recordMapper.selectById(1L)).thenReturn(record(1L, ProductionConstants.ORDER_TYPE_MEDICAL));
        when(productMapper.selectCount(any())).thenReturn(2L);
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_ALL_PASS.getCode(),
                assertThrows(BusinessException.class, () -> qcService.transferToPacking(1L)).getCode());
    }

    @Test
    void transferToPacking_allPass_updatesStatusAndTriggersFlow() {
        ProductionRecordEntity rec = record(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        rec.setOrderId(10L);
        when(recordMapper.selectById(1L)).thenReturn(rec);
        when(productMapper.selectCount(any())).thenReturn(0L);

        qcService.transferToPacking(1L);

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                RecordStatusEnum.PACKING.getCode().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                RecordStatusEnum.PACKING.getCode(), FlowActionEnum.QC_PASS);
    }

    // ---- listProductsByRecordId ----

    @Test
    void listProductsByRecordId_returnsConvertedList() {
        ProductionProductEntity p = product(1L, 10L);
        p.setProductNo("P-001");
        when(productMapper.selectList(any())).thenReturn(List.of(p));
        List<ProductionProductVO> result = qcService.listProductsByRecordId(10L);
        assertEquals(1, result.size());
        assertEquals("P-001", result.get(0).getProductNo());
    }

    // ---- listQcRecords ----

    @Test
    void listQcRecords_noStatusFilter_usesQcInProgress() {
        ProductionQcPageDTO dto = new ProductionQcPageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(recordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        qcService.listQcRecords(dto);
        verify(recordMapper).selectPage(any(), any());
    }

    @Test
    void listQcRecords_withStatusFilter_overridesDefault() {
        ProductionQcPageDTO dto = new ProductionQcPageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setStatus(RecordStatusEnum.PACKING.getCode());
        when(recordMapper.selectPage(any(), any())).thenReturn(new Page<>());
        qcService.listQcRecords(dto);
        verify(recordMapper).selectPage(any(), any());
    }

    // ---- listRedoProducts ----

    @Test
    void listRedoProducts_noRecordIdFilter_queriesAllRedo() {
        ProductionRedoPageDTO dto = new ProductionRedoPageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(productMapper.selectPage(any(), any())).thenReturn(new Page<>());
        qcService.listRedoProducts(dto);
        verify(productMapper).selectPage(any(), any());
    }

    @Test
    void listRedoProducts_withRecordIdFilter_addsCondition() {
        ProductionRedoPageDTO dto = new ProductionRedoPageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setRecordId(5L);
        when(productMapper.selectPage(any(), any())).thenReturn(new Page<>());
        qcService.listRedoProducts(dto);
        verify(productMapper).selectPage(any(), any());
    }

    // ---- helpers ----

    private ProductionProductEntity product(Long id, Long recordId) {
        ProductionProductEntity p = new ProductionProductEntity();
        p.setId(id);
        p.setProductionRecordId(recordId);
        p.setProductNo("P-00" + id);
        return p;
    }

    private ProductionRecordEntity record(Long id, Integer orderType) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(id * 10);
        r.setOrderType(orderType);
        r.setRecordNo("REC-00" + id);
        return r;
    }
}
