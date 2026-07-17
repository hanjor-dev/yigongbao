package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.module.production.helper.ProductLedgerExcelBuilder;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionRecordAssignDeviceTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignPackageFileMapper designPackageFileMapper;
    @Mock private DesignInstructionMapper designInstructionMapper;
    @Mock private DesignDrawingMapper designDrawingMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private FlowFacade flowFacade;
    @Mock private FlowCardExcelBuilder flowCardExcelBuilder;
    @Mock private ProductLedgerExcelBuilder productLedgerExcelBuilder;
    @Mock private FileService fileService;
    @Mock private ConfigService configService;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private IDeviceUsageCounterService deviceUsageCounterService;
    @Mock private IProductNumberService productNumberService;
    @Mock private ProductionProcessMapper processMapper;

    @Spy
    @InjectMocks
    private ProductionRecordServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductionRecordEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductionProcessEntity.class);
    }

    @BeforeEach
    void setBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
    }

    @Test
    void assignDevice_rejectsRecordNotFound() {
        doReturn(null).when(service).getById(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, new AssignDeviceDTO()));

        assertThat(exception.getMessage()).contains("流转卡");
        verifyNoInteractions(deviceMapper, processMapper, productNumberService);
    }

    @Test
    void assignDevice_rejectsOfflineDevice() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = new DeviceEntity();
        device.setId(2L);
        device.setConnectionStatus(0);
        when(deviceMapper.selectById(2L)).thenReturn(device);
        doReturn(record).when(service).getById(1L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, dto));

        assertThat(exception.getMessage()).contains("设备");
        verifyNoInteractions(processMapper, productNumberService);
    }

    @Test
    void assignDevice_successUpdatesProcessAndGeneratesFormalNumbers() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = new DeviceEntity();
        device.setId(2L);
        device.setDeviceId("SLA-002");
        device.setDeviceName("打印机2");
        device.setState(0);
        device.setConnectionStatus(1);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setMaterial("树脂");
        UserEntity user = new UserEntity();
        user.setRealName("生产员");
        doReturn(record).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(ProductionRecordEntity.class));
        when(deviceMapper.selectById(2L)).thenReturn(device);
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(deviceUsageCounterService.incrementAndGet(2L)).thenReturn(3);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            service.assignDevice(1L, dto);
        }

        assertThat(record.getPrintDeviceId()).isEqualTo(2L);
        assertThat(record.getPrintDeviceCode()).isEqualTo("SLA-002");
        verify(processMapper).update(isNull(), any());
        verify(deviceUsageCounterService).incrementAndGet(2L);
        verify(productNumberService).generateFormalNumbers(1L, 2L, 3);
    }

    private ProductionRecordEntity pendingRecord(Long id) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(id);
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        record.setOrderId(10L);
        return record;
    }
}
