package com.yigongbao.module.production.qc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.qc.dto.BatchUpdateUdiDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 批量更新UDI单元测试
 *
 * @author hanjor
 * @date 2026-07-13
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchUpdateUdiTest {

    @Mock
    private ProductionRecordMapper recordMapper;

    @Mock
    private ProductionProductMapper productMapper;

    @Mock
    private IProductionProductService productService;

    @InjectMocks
    private ProductionQcServiceImpl qcService;

    @Captor
    private ArgumentCaptor<Collection<ProductionProductEntity>> productListCaptor;

    /**
     * 场景1: 状态校验 - 待打印状态不允许更新
     * Given: 流转卡状态为待打印（< PRINTING）
     * When: 调用batchUpdateUdi
     * Then: 抛出RECORD_STATUS_NOT_ALLOW_UPDATE_UDI异常
     */
    @Test
    void batchUpdateUdi_statusBeforePrinting_throwsException() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue()); // 3010 < 3020
        when(recordMapper.selectById(1L)).thenReturn(record);

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001")
        ));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> qcService.batchUpdateUdi(dto));
        assertEquals(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_UPDATE_UDI.getCode(), exception.getCode());

        // 验证未调用更新操作
        verify(productService, never()).updateBatchById(anyList());
    }

    /**
     * 场景2: 状态校验 - 打印中状态允许更新
     * Given: 流转卡状态为打印中（>= PRINTING）
     * When: 调用batchUpdateUdi
     * Then: 成功更新UDI
     */
    @Test
    void batchUpdateUdi_statusPrinting_success() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.PRINTING.getValue()); // 3020 >= 3020
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L); // 无重复UDI

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001")
        ));

        // When
        qcService.batchUpdateUdi(dto);

        // Then
        verify(productService).updateBatchById(productListCaptor.capture());
        List<ProductionProductEntity> captured = (List<ProductionProductEntity>) productListCaptor.getValue();
        assertEquals(1, captured.size());
        assertEquals(101L, captured.get(0).getId());
        assertEquals("UDI-001", captured.get(0).getUdiCode());
    }

    /**
     * 场景3: 订单类型校验 - 非医疗器械不允许
     * Given: 流转卡订单类型为非医疗器械
     * When: 调用batchUpdateUdi
     * Then: 抛出NON_MEDICAL_NOT_ALLOW_UDI异常
     */
    @Test
    void batchUpdateUdi_nonMedicalOrderType_throwsException() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_NON_MEDICAL);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001")
        ));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> qcService.batchUpdateUdi(dto));
        assertEquals(ErrorCodeEnum.NON_MEDICAL_NOT_ALLOW_UDI.getCode(), exception.getCode());

        // 验证未调用更新操作
        verify(productService, never()).updateBatchById(anyList());
    }

    /**
     * 场景4: 唯一性校验 - 重复UDI抛出异常
     * Given: 数据库中已存在相同UDI码（其他产品）
     * When: 调用batchUpdateUdi
     * Then: 抛出UDI_CODE_EXISTS异常
     */
    @Test
    void batchUpdateUdi_duplicateUdi_throwsException() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // 存在重复UDI

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001")
        ));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> qcService.batchUpdateUdi(dto));
        assertEquals(ErrorCodeEnum.UDI_CODE_EXISTS.getCode(), exception.getCode());

        // 验证未调用更新操作
        verify(productService, never()).updateBatchById(anyList());
    }

    /**
     * 场景5: 唯一性校验 - 同一产品更新自己的UDI允许
     * Given: UDI码只在当前产品中存在（更新自己的UDI）
     * When: 调用batchUpdateUdi
     * Then: 成功更新UDI
     */
    @Test
    void batchUpdateUdi_updateOwnUdi_success() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L); // 排除自己后无重复

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001-UPDATED")
        ));

        // When
        qcService.batchUpdateUdi(dto);

        // Then
        verify(productService).updateBatchById(productListCaptor.capture());
        List<ProductionProductEntity> captured = (List<ProductionProductEntity>) productListCaptor.getValue();
        assertEquals(1, captured.size());
        assertEquals(101L, captured.get(0).getId());
        assertEquals("UDI-001-UPDATED", captured.get(0).getUdiCode());
        assertNotNull(captured.get(0).getUdiGenerateTime());
    }

    /**
     * 场景6: 批量更新 - 多个产品成功更新
     * Given: 多个产品需要更新UDI，无重复
     * When: 调用batchUpdateUdi
     * Then: 成功批量更新所有产品
     */
    @Test
    void batchUpdateUdi_multipleProducts_success() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.QC_IN_PROGRESS.getValue()); // 5010 >= 3020
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L); // 无重复UDI

        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "UDI-001"),
            createProductUdiItem(102L, "UDI-002"),
            createProductUdiItem(103L, "UDI-003")
        ));

        // When
        qcService.batchUpdateUdi(dto);

        // Then
        verify(productService).updateBatchById(productListCaptor.capture());
        List<ProductionProductEntity> captured = (List<ProductionProductEntity>) productListCaptor.getValue();
        assertEquals(3, captured.size());
        assertEquals("UDI-001", captured.get(0).getUdiCode());
        assertEquals("UDI-002", captured.get(1).getUdiCode());
        assertEquals("UDI-003", captured.get(2).getUdiCode());
    }

    /**
     * 场景7: 空值校验 - UDI为空抛出异常
     * Given: UDI码为空字符串
     * When: 调用batchUpdateUdi
     * Then: 验证注解会在Controller层拦截（此测试验证Service层不会处理空值）
     * 注: 实际空值校验由@NotBlank注解在Controller层完成
     */
    @Test
    void batchUpdateUdi_emptyUdi_serviceLayerProcessesAsIs() {
        // Given
        ProductionRecordEntity record = createRecord(1L, ProductionConstants.ORDER_TYPE_MEDICAL);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 注: 在实际场景中，空字符串会被@NotBlank拦截，但Service层不做校验
        BatchUpdateUdiDTO dto = createBatchUpdateUdiDTO(1L, Arrays.asList(
            createProductUdiItem(101L, "") // 空字符串
        ));

        // When
        qcService.batchUpdateUdi(dto);

        // Then: Service层正常处理，实际空值应由Controller层@Valid注解拦截
        verify(productService).updateBatchById(productListCaptor.capture());
        List<ProductionProductEntity> captured = (List<ProductionProductEntity>) productListCaptor.getValue();
        assertEquals(1, captured.size());
        assertEquals("", captured.get(0).getUdiCode());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建流转卡实体
     */
    private ProductionRecordEntity createRecord(Long id, Integer orderType) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(id);
        record.setRecordNo("REC-00" + id);
        record.setOrderId(id * 10);
        record.setOrderType(orderType);
        record.setStatus(FlowStatusEnum.PRINTING.getValue()); // 默认打印中状态
        return record;
    }

    /**
     * 创建批量更新UDI DTO
     */
    private BatchUpdateUdiDTO createBatchUpdateUdiDTO(Long recordId, List<BatchUpdateUdiDTO.ProductUdiItem> products) {
        BatchUpdateUdiDTO dto = new BatchUpdateUdiDTO();
        dto.setRecordId(recordId);
        dto.setProducts(products);
        return dto;
    }

    /**
     * 创建产品UDI项
     */
    private BatchUpdateUdiDTO.ProductUdiItem createProductUdiItem(Long productId, String udiCode) {
        BatchUpdateUdiDTO.ProductUdiItem item = new BatchUpdateUdiDTO.ProductUdiItem();
        item.setProductId(productId);
        item.setUdiCode(udiCode);
        return item;
    }
}
