package com.yigongbao.common.constant;

import java.util.Objects;

/**
 * 订单实体交付类型及其流程语义。
 */
public final class PhysicalDeliveryConstants {

    private PhysicalDeliveryConstants() {
    }

    /** 不需要实体交付。 */
    public static final int NO_PHYSICAL_DELIVERY = 0;

    /** 需要实体交付，走完整生产流程。 */
    public static final int NEEDS_PHYSICAL_DELIVERY = 1;

    /** 异地打印，复用不需要实体交付流程。 */
    public static final int REMOTE_PRINTING = 2;

    public static boolean isSupported(Integer value) {
        return Objects.equals(value, NO_PHYSICAL_DELIVERY)
                || Objects.equals(value, NEEDS_PHYSICAL_DELIVERY)
                || Objects.equals(value, REMOTE_PRINTING);
    }

    public static boolean needsProduction(Integer value) {
        return value == null || Objects.equals(value, NEEDS_PHYSICAL_DELIVERY);
    }

    public static boolean isNoPhysicalDelivery(Integer value) {
        return Objects.equals(value, NO_PHYSICAL_DELIVERY)
                || Objects.equals(value, REMOTE_PRINTING);
    }

    public static String getDisplayName(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case NO_PHYSICAL_DELIVERY -> "不需要实体交付";
            case NEEDS_PHYSICAL_DELIVERY -> "需要实体交付";
            case REMOTE_PRINTING -> "异地打印";
            default -> null;
        };
    }
}
