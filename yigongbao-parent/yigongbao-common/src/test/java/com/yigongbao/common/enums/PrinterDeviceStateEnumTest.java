package com.yigongbao.common.enums;

import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrinterDeviceStateEnumTest {

    @Test
    void fromCode_returnsConfiguredStateName() {
        assertEquals("空闲", PrinterDeviceStateEnum.fromCode(0).getName());
        assertEquals("工作中", PrinterDeviceStateEnum.fromCode(1).getName());
        assertEquals("打印完成", PrinterDeviceStateEnum.fromCode(2).getName());
        assertEquals("报警", PrinterDeviceStateEnum.fromCode(3).getName());
        assertEquals("暂停", PrinterDeviceStateEnum.fromCode(4).getName());
        assertEquals("准备就绪", PrinterDeviceStateEnum.fromCode(5).getName());
        assertEquals("离线", PrinterDeviceStateEnum.fromCode(6).getName());
    }

    @Test
    void isValid_rejectsNullAndUnknownCodes() {
        assertFalse(PrinterDeviceStateEnum.isValid(null));
        assertFalse(PrinterDeviceStateEnum.isValid(-1));
        assertFalse(PrinterDeviceStateEnum.isValid(7));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void isValid_acceptsConfiguredCodes(int code) {
        assertTrue(PrinterDeviceStateEnum.isValid(code));
    }

    @Test
    void isAssignableState_acceptsOnlyIdle() {
        assertTrue(PrinterDeviceStateEnum.IDLE.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.WORKING.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.PRINT_FINISHED.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.ALARM.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.PAUSED.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.READY.isAssignableState());
        assertFalse(PrinterDeviceStateEnum.OFFLINE.isAssignableState());
    }

    @Test
    void isInUse_returnsTrueForActiveDevice() {
        PrinterDeviceUsageChecker checker = deviceIds -> Set.copyOf(deviceIds);

        assertTrue(checker.isInUse(42L));
    }

    @Test
    void isInUse_returnsFalseForInactiveDevice() {
        PrinterDeviceUsageChecker checker = deviceIds -> Set.of();

        assertFalse(checker.isInUse(42L));
    }

    @Test
    void isInUse_returnsFalseForNullWithoutQuerying() {
        PrinterDeviceUsageChecker checker = deviceIds -> {
            throw new AssertionError("Null device must not trigger a usage query");
        };

        assertFalse(checker.isInUse(null));
    }

    @Test
    void isInUse_failsClosedWhenImplementationReturnsNull() {
        PrinterDeviceUsageChecker checker = deviceIds -> null;

        assertThrows(NullPointerException.class, () -> checker.isInUse(42L));
    }
}
