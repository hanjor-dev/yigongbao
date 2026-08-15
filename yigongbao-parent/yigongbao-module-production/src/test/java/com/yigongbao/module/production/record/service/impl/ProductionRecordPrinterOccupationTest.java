package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.PrinterRecordUsageChecker;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.PrinterOccupationVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionRecordPrinterOccupationTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private PrinterRecordUsageChecker usageChecker;

    @InjectMocks private ProductionRecordServiceImpl service;

    @BeforeEach
    void setBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
    }

    @Test
    void getPrinterOccupation_returnsTrueForAuthorizedUserAndPassesIdsToChecker() {
        stubRecordAndSlaDevice();
        when(usageChecker.isInUseByOtherRecord(8L, 7L)).thenReturn(true);

        PrinterOccupationVO result = runAsAdmin(() -> service.getPrinterOccupation(7L, 8L));

        assertThat(result.getOccupied()).isTrue();
        verify(usageChecker).isInUseByOtherRecord(8L, 7L);
    }

    @Test
    void getPrinterOccupation_returnsFalseWhenNoOtherRecordUsesPrinter() {
        stubRecordAndSlaDevice();
        when(usageChecker.isInUseByOtherRecord(8L, 7L)).thenReturn(false);

        PrinterOccupationVO result = runAsAdmin(() -> service.getPrinterOccupation(7L, 8L));

        assertThat(result.getOccupied()).isFalse();
    }

    @Test
    void getPrinterOccupation_rejectsMissingRecordBeforeDeviceLookup() {
        when(recordMapper.selectById(7L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPrinterOccupation(7L, 8L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode());
        verifyNoInteractions(deviceMapper, userMapper, usageChecker);
    }

    @Test
    void getPrinterOccupation_rejectsMissingDevice() {
        when(recordMapper.selectById(7L)).thenReturn(record());
        when(deviceMapper.selectById(8L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPrinterOccupation(7L, 8L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND.getCode());
        verifyNoInteractions(userMapper, usageChecker);
    }

    @Test
    void getPrinterOccupation_rejectsNonSlaDeviceWithoutCheckingUsage() {
        DeviceEntity device = device();
        device.setDeviceType("OVEN");
        when(recordMapper.selectById(7L)).thenReturn(record());
        when(deviceMapper.selectById(8L)).thenReturn(device);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPrinterOccupation(7L, 8L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_TYPE_MISMATCH.getCode());
        verifyNoInteractions(userMapper, usageChecker);
    }

    @Test
    void getPrinterOccupation_rejectsUnauthorizedRoleBeforeCheckingUsage() {
        stubRecordAndSlaDevice();
        UserEntity user = new UserEntity();
        user.setRoleCode(RoleCodeEnum.FINANCE.getCode());
        when(userMapper.selectById(11L)).thenReturn(user);

        BusinessException exception = runAs(11L, () -> assertThrows(BusinessException.class,
                () -> service.getPrinterOccupation(7L, 8L)));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.FORBIDDEN.getCode());
        verifyNoInteractions(usageChecker);
    }

    @Test
    void getPrinterOccupation_rejectsCrossCenterAccessBeforeCheckingUsage() {
        stubRecordAndSlaDevice();
        UserEntity worker = new UserEntity();
        worker.setRoleCode(RoleCodeEnum.PRODUCTION_WORKER.getCode());
        worker.setCenterId(100L);
        when(userMapper.selectById(11L)).thenReturn(worker);
        OrderMainEntity order = new OrderMainEntity();
        order.setCenterId(200L);
        when(orderMainMapper.selectById(10L)).thenReturn(order);

        BusinessException exception = runAs(11L, () -> assertThrows(BusinessException.class,
                () -> service.getPrinterOccupation(7L, 8L)));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.FORBIDDEN.getCode());
        verifyNoInteractions(usageChecker);
    }

    private void stubRecordAndSlaDevice() {
        when(recordMapper.selectById(7L)).thenReturn(record());
        when(deviceMapper.selectById(8L)).thenReturn(device());
    }

    private ProductionRecordEntity record() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(7L);
        record.setOrderId(10L);
        return record;
    }

    private DeviceEntity device() {
        DeviceEntity device = new DeviceEntity();
        device.setId(8L);
        device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        device.setCenterId(100L);
        device.setConnectionStatus(0);
        device.setState(1);
        return device;
    }

    private <T> T runAsAdmin(java.util.function.Supplier<T> action) {
        UserEntity admin = new UserEntity();
        admin.setRoleCode(RoleCodeEnum.ADMIN.getCode());
        when(userMapper.selectById(11L)).thenReturn(admin);
        return runAs(11L, action);
    }

    private <T> T runAs(Long userId, java.util.function.Supplier<T> action) {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            return action.get();
        }
    }
}
