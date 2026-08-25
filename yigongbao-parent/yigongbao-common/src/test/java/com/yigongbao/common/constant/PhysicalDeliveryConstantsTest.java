package com.yigongbao.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalDeliveryConstantsTest {

    @Test
    void remotePrintingShouldUseNonProductionFlow() {
        assertFalse(PhysicalDeliveryConstants.needsProduction(PhysicalDeliveryConstants.REMOTE_PRINTING));
        assertTrue(PhysicalDeliveryConstants.isNoPhysicalDelivery(PhysicalDeliveryConstants.REMOTE_PRINTING));
    }

    @Test
    void onlyPhysicalDeliveryShouldUseProductionFlow() {
        assertTrue(PhysicalDeliveryConstants.needsProduction(PhysicalDeliveryConstants.NEEDS_PHYSICAL_DELIVERY));
        assertFalse(PhysicalDeliveryConstants.isNoPhysicalDelivery(PhysicalDeliveryConstants.NEEDS_PHYSICAL_DELIVERY));
    }

    @Test
    void displayNameShouldPreserveRemotePrintingMeaning() {
        assertEquals("异地打印", PhysicalDeliveryConstants.getDisplayName(PhysicalDeliveryConstants.REMOTE_PRINTING));
        assertEquals("不需要实体交付", PhysicalDeliveryConstants.getDisplayName(PhysicalDeliveryConstants.NO_PHYSICAL_DELIVERY));
        assertEquals("需要实体交付", PhysicalDeliveryConstants.getDisplayName(PhysicalDeliveryConstants.NEEDS_PHYSICAL_DELIVERY));
    }
}
