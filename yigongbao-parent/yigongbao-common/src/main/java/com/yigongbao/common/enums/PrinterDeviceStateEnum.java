package com.yigongbao.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 打印设备状态。
 */
@Getter
@RequiredArgsConstructor
public enum PrinterDeviceStateEnum {

    IDLE(0, "空闲"),
    WORKING(1, "工作中"),
    PRINT_FINISHED(2, "打印完成"),
    ALARM(3, "报警"),
    PAUSED(4, "暂停"),
    READY(5, "准备就绪"),
    OFFLINE(6, "离线");

    private final Integer code;
    private final String name;

    public static PrinterDeviceStateEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(state -> state.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }

    public boolean isAssignableState() {
        return this == IDLE;
    }
}
