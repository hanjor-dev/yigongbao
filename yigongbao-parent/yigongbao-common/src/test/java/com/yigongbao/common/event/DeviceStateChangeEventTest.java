package com.yigongbao.common.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeviceStateChangeEventTest {

    @Test
    void oldConstructorKeepsOptionalPrintMetadataNull() {
        DeviceStateChangeEvent event = new DeviceStateChangeEvent(this, 1L, 0, 1);

        assertEquals(1L, event.getDeviceId());
        assertEquals(0, event.getOldState());
        assertEquals(1, event.getNewState());
        assertNull(event.getPrintStartTime());
        assertNull(event.getEstimatedDurationMinutes());
        assertNull(event.getEstimatedPrintFinishTime());
    }

    @Test
    void newConstructorExposesPrintMetadata() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 25, 10, 20, 30);
        LocalDateTime finish = start.plusMinutes(90);

        DeviceStateChangeEvent event = new DeviceStateChangeEvent(
                this, 1L, 0, 1, start, 90, finish);

        assertEquals(start, event.getPrintStartTime());
        assertEquals(90, event.getEstimatedDurationMinutes());
        assertEquals(finish, event.getEstimatedPrintFinishTime());
    }
}
