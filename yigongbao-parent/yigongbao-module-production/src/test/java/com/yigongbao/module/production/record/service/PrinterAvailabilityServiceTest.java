package com.yigongbao.module.production.record.service;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.production.record.vo.PrinterVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrinterAvailabilityServiceTest {

    @Mock
    private PrinterDeviceUsageChecker usageChecker;

    private PrinterAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new PrinterAvailabilityService(usageChecker);
    }

    @Test
    void isAvailable_onlyForOnlineIdleDeviceWithoutActiveUsage() {
        DeviceEntity device = device(1L, 1, 0);

        assertThat(service.isAvailable(device, false)).isTrue();
        assertThat(service.isAvailable(device, true)).isFalse();
    }

    @Test
    void isAvailable_rejectsOfflineUnknownConnectionAndNullState() {
        assertThat(service.isAvailable(device(1L, 0, 0), false)).isFalse();
        assertThat(service.isAvailable(device(2L, null, 0), false)).isFalse();
        assertThat(service.isAvailable(device(3L, 1, null), false)).isFalse();
    }

    @Test
    void isAvailable_rejectsEveryNonIdlePrinterStateIncludingReady() {
        for (int state = 1; state <= 6; state++) {
            assertThat(service.isAvailable(device((long) state, 1, state), false))
                    .as("state %s must not be assignable", state)
                    .isFalse();
        }
    }

    @Test
    void requireAvailable_throwsDeviceNotAvailable() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireAvailable(device(1L, 1, 5), false));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
    }

    @Test
    void toPrinterVOs_bulkLoadsUsageOnceAndPreservesLegacyStatus() {
        DeviceEntity free = device(1L, 1, 0);
        free.setDeviceId("SLA-001");
        free.setDeviceName("打印机1");
        DeviceEntity occupied = device(2L, 1, 0);
        occupied.setDeviceId("SLA-002");
        occupied.setDeviceName("打印机2");
        when(usageChecker.findActiveDeviceIds(List.of(1L, 2L))).thenReturn(Set.of(2L));

        List<PrinterVO> result = service.toPrinterVOs(List.of(free, occupied));

        assertThat(result).extracting(PrinterVO::getStatus).containsExactly(0, 1);
        assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("空闲", "占用");
        assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(true, false);
        assertThat(result).extracting(PrinterVO::getDeviceState).containsExactly(0, 0);
        assertThat(result).extracting(PrinterVO::getDeviceStateName).containsExactly("空闲", "空闲");
        assertThat(result).extracting(PrinterVO::getConnectionStatus).containsExactly(1, 1);
        verify(usageChecker).findActiveDeviceIds(List.of(1L, 2L));
    }

    @Test
    void toPrinterVOs_usesFullPrinterStateNamesAndLeavesUnknownStateNameNull() {
        DeviceEntity ready = device(1L, 1, 5);
        DeviceEntity unknown = device(2L, 1, 99);
        DeviceEntity missing = device(3L, 1, null);
        when(usageChecker.findActiveDeviceIds(List.of(1L, 2L, 3L))).thenReturn(Set.of());

        List<PrinterVO> result = service.toPrinterVOs(List.of(ready, unknown, missing));

        assertThat(result).extracting(PrinterVO::getDeviceStateName)
                .containsExactly("准备就绪", null, null);
        assertThat(result).extracting(PrinterVO::getStatus).containsExactly(1, 1, 1);
        assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("占用", "占用", "占用");
        assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(false, false, false);
    }

    private DeviceEntity device(Long id, Integer connectionStatus, Integer state) {
        DeviceEntity device = new DeviceEntity();
        device.setId(id);
        device.setConnectionStatus(connectionStatus);
        device.setState(state);
        return device;
    }
}
