package com.yigongbao.module.production.record.service;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.production.record.vo.PrinterVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrinterAvailabilityServiceTest {

    private final PrinterAvailabilityService service = new PrinterAvailabilityService();

    @Test
    void isAvailable_onlyForOnlineIdleSlaPrinter() {
        DeviceEntity device = device(1L, 1, 0);

        assertThat(service.isAvailable(device)).isTrue();
        device.setConnectionStatus(0);
        assertThat(service.isAvailable(device)).isFalse();
        device.setConnectionStatus(null);
        assertThat(service.isAvailable(device)).isFalse();
        device.setDeviceType(DeviceTypeEnum.WASH_CONTAINER.getCode());
        assertThat(service.isAvailable(device)).isFalse();
    }

    @Test
    void isAvailable_rejectsOfflineUnknownConnectionAndNullState() {
        assertThat(service.isAvailable(device(1L, 0, 0))).isFalse();
        assertThat(service.isAvailable(device(2L, null, 0))).isFalse();
        assertThat(service.isAvailable(device(3L, 1, null))).isFalse();
        assertThat(service.isAvailable(null)).isFalse();
    }

    @Test
    void toPrinterVOsIgnoringConnection_marksOfflineIdlePrinterAvailable() {
        DeviceEntity device = device(1L, 0, 0);

        List<PrinterVO> result = service.toPrinterVOsIgnoringConnection(List.of(device));

        assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(true);
        assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("空闲");
        assertThat(result).extracting(PrinterVO::getConnectionStatus).containsExactly(0);
    }

    @Test
    void isAvailable_rejectsEveryNonIdlePrinterStateIncludingReady() {
        for (int state = 1; state <= 6; state++) {
            assertThat(service.isAvailable(device((long) state, 1, state)))
                    .as("state %s must not be assignable", state)
                    .isFalse();
        }
    }

    @Test
    void requireAvailable_throwsDeviceNotAvailable() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireAvailable(device(1L, 1, 5)));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
    }

    @Test
    void requireAvailable_rejectsNonSlaPrinterWithTypeMismatch() {
        DeviceEntity device = device(1L, 1, 0);
        device.setDeviceType(DeviceTypeEnum.WASH_CONTAINER.getCode());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireAvailable(device));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_TYPE_MISMATCH.getCode());
    }

    @Test
    void requireAvailable_rejectsNullWithoutThrowingNullPointerException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireAvailable(null));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
    }

    @Test
    void toPrinterVOs_ignoresProductionUsageAndMapsPhysicalAvailability() {
        DeviceEntity free = device(1L, 1, 0);
        free.setDeviceId("SLA-001");
        free.setDeviceName("打印机1");
        DeviceEntity active = device(2L, 1, 0);
        active.setDeviceId("SLA-002");
        active.setDeviceName("打印机2");

        List<PrinterVO> result = service.toPrinterVOs(List.of(free, active));

        assertThat(result).extracting(PrinterVO::getStatus).containsExactly(0, 0);
        assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("空闲", "空闲");
        assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(true, true);
        assertThat(result).extracting(PrinterVO::getDeviceState).containsExactly(0, 0);
        assertThat(result).extracting(PrinterVO::getDeviceStateName).containsExactly("空闲", "空闲");
        assertThat(result).extracting(PrinterVO::getConnectionStatus).containsExactly(1, 1);
    }

    @Test
    void toPrinterVOs_usesFullPrinterStateNamesAndLeavesUnknownStateNameNull() {
        DeviceEntity ready = device(1L, 1, 5);
        DeviceEntity unknown = device(2L, 1, 99);
        DeviceEntity missing = device(3L, 1, null);
        List<PrinterVO> result = service.toPrinterVOs(List.of(ready, unknown, missing));

        assertThat(result).extracting(PrinterVO::getDeviceStateName)
                .containsExactly("准备就绪", null, null);
        assertThat(result).extracting(PrinterVO::getStatus).containsExactly(1, 1, 1);
        assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("不可用", "不可用", "不可用");
        assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(false, false, false);
    }

    private DeviceEntity device(Long id, Integer connectionStatus, Integer state) {
        DeviceEntity device = new DeviceEntity();
        device.setId(id);
        device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        device.setConnectionStatus(connectionStatus);
        device.setState(state);
        return device;
    }
}
