package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.module.production.helper.ProductLedgerExcelBuilder;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionRecordLedgerDateValidationTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignInstructionMapper designInstructionMapper;
    @Mock private DesignDrawingMapper designDrawingMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private FlowFacade flowFacade;
    @Mock private FlowCardExcelBuilder flowCardExcelBuilder;
    @Mock private ProductLedgerExcelBuilder productLedgerExcelBuilder;
    @Mock private FileService fileService;
    @Mock private ConfigService configService;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private IDeviceUsageCounterService deviceUsageCounterService;
    @Mock private IProductNumberService productNumberService;

    @InjectMocks
    private ProductionRecordServiceImpl service;

    @Test
    void exportNormalizesNonMidnightEndTimeToNextDayMidnight() throws Exception {
        ProductLedgerExportDTO dto = new ProductLedgerExportDTO();
        dto.setStartTime(LocalDateTime.of(2026, 8, 13, 15, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 13, 18, 30));
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
        when(productMapper.countProductLedgerData(any())).thenReturn(1L);
        when(productMapper.listProductLedgerData(any())).thenReturn(List.of(Map.of("product_no", "P-1")));
        when(productLedgerExcelBuilder.build(any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(new byte[]{1, 2});

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            assertArrayEquals(new byte[]{1, 2}, service.exportProductLedger(dto));
        }

        ArgumentCaptor<ProductLedgerExportDTO> countCaptor = ArgumentCaptor.forClass(ProductLedgerExportDTO.class);
        ArgumentCaptor<ProductLedgerExportDTO> listCaptor = ArgumentCaptor.forClass(ProductLedgerExportDTO.class);
        verify(productMapper).countProductLedgerData(countCaptor.capture());
        verify(productMapper).listProductLedgerData(listCaptor.capture());
        LocalDateTime expectedExclusiveEnd = LocalDateTime.of(2026, 8, 14, 0, 0);
        assertEquals(expectedExclusiveEnd, countCaptor.getValue().getEndTime());
        assertEquals(expectedExclusiveEnd, listCaptor.getValue().getEndTime());
    }

    @Test
    void repeatedExportDoesNotMutateOrExpandTheOriginalDateRange() throws Exception {
        ProductLedgerExportDTO dto = new ProductLedgerExportDTO();
        dto.setStartTime(LocalDateTime.of(2026, 8, 13, 0, 0));
        LocalDateTime originalEndTime = LocalDateTime.of(2026, 8, 13, 18, 30);
        dto.setEndTime(originalEndTime);
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
        when(productMapper.countProductLedgerData(any())).thenReturn(1L);
        when(productMapper.listProductLedgerData(any())).thenReturn(List.of(Map.of("product_no", "P-1")));
        when(productLedgerExcelBuilder.build(any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(new byte[]{1});

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            service.exportProductLedger(dto);
            service.exportProductLedger(dto);
        }

        ArgumentCaptor<ProductLedgerExportDTO> queryCaptor = ArgumentCaptor.forClass(ProductLedgerExportDTO.class);
        verify(productMapper, times(2)).countProductLedgerData(queryCaptor.capture());
        LocalDateTime expectedExclusiveEnd = LocalDateTime.of(2026, 8, 14, 0, 0);
        assertEquals(List.of(expectedExclusiveEnd, expectedExclusiveEnd),
                queryCaptor.getAllValues().stream().map(ProductLedgerExportDTO::getEndTime).toList());
        assertEquals(originalEndTime, dto.getEndTime());
    }

    @Test
    void exportRejectsStartAtNextDayMidnight() {
        ProductLedgerExportDTO dto = new ProductLedgerExportDTO();
        dto.setStartTime(LocalDateTime.of(2026, 8, 14, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 13, 15, 0));
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.exportProductLedger(dto));

            assertEquals("开始时间不能晚于结束时间", exception.getMessage());
        }
        verify(productMapper, never()).countProductLedgerData(any());
        verify(productMapper, never()).listProductLedgerData(any());
    }
}
