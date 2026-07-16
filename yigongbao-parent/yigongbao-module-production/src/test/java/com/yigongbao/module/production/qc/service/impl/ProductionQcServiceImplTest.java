package com.yigongbao.module.production.qc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.user.mapper.UserMapper;
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
    @Mock private UserMapper userMapper;

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
    void markProductPass_noUdiGenerated() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        when(recordMapper.selectById(10L)).thenReturn(record(10L, ProductionConstants.ORDER_TYPE_NON_MEDICAL));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductPass(1L);
        }
        verify(codeGeneratorService, never()).generate(ProductionConstants.UDI_CODE);
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                ProductStatusEnum.PASS.getCode().equals(((ProductionProductEntity) p).getStatus())
                        && QcResultEnum.PASS.getCode().equals(((ProductionProductEntity) p).getQcResult())));
    }


    @Test
    void markProductPass_failStatus_allowed() {
        ProductionProductEntity p = product(1L, 10L);
        p.setStatus(ProductStatusEnum.FAIL.getCode());
        when(productMapper.selectById(1L)).thenReturn(p);
        when(recordMapper.selectById(10L)).thenReturn(record(10L, ProductionConstants.ORDER_TYPE_NON_MEDICAL));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductPass(1L);
        }
        verify(productMapper).updateById((ProductionProductEntity) argThat(p2 ->
                ProductStatusEnum.PASS.getCode().equals(((ProductionProductEntity) p2).getStatus())));
    }

    // ---- markProductFail ----

    @Test
    void markProductFail_productNotFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(),
                    assertThrows(BusinessException.class, () -> qcService.markProductFail(99L, "defect")).getCode());
        }
    }

    @Test
    void markProductFail_setsFailStatusAndRemark() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.markProductFail(1L, "surface defect");
        }
        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                ProductStatusEnum.FAIL.getCode().equals(((ProductionProductEntity) p).getStatus())
                        && QcResultEnum.FAIL.getCode().equals(((ProductionProductEntity) p).getQcResult())
                        && "surface defect".equals(((ProductionProductEntity) p).getQcRemark())));
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
        assertEquals(ErrorCodeEnum.PRODUCT_HAS_NOT_QC.getCode(),
                assertThrows(BusinessException.class, () -> qcService.transferToPacking(1L)).getCode());
    }

    @Test
    void transferToPacking_allPass_updatesStatusAndTriggersFlow() {
        ProductionRecordEntity rec = record(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        rec.setOrderId(10L);
        when(recordMapper.selectById(1L)).thenReturn(rec);
        when(productMapper.selectCount(any())).thenReturn(0L);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.transferToPacking(1L);
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.PACKING.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PACKING.getValue(), FlowActionEnum.QC_PASS);
        verify(recordService).reconcileOrderProductionStatus(10L);
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
        when(recordService.pageRecords(any())).thenReturn(new Page<>());
        qcService.listQcRecords(dto);
        verify(recordService).pageRecords(argThat(pageDTO ->
                FlowStatusEnum.QC_IN_PROGRESS.getValue().equals(pageDTO.getStatus())
                        && Boolean.TRUE.equals(pageDTO.getIncludeFollowingStatuses())));
    }

    // ---- helpers ----

    private ProductionProductEntity product(Long id, Long recordId) {
        ProductionProductEntity p = new ProductionProductEntity();
        p.setId(id);
        p.setProductionRecordId(recordId);
        p.setProductNo("P-00" + id);
        p.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
        return p;
    }

    private ProductionRecordEntity record(Long id, Integer orderType) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(id * 10);
        r.setOrderType(orderType);
        r.setRecordNo("REC-00" + id);
        r.setStatus(FlowStatusEnum.QC_IN_PROGRESS.getValue());
        return r;
    }
}
