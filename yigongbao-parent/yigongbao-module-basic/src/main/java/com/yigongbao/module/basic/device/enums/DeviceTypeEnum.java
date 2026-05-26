package com.yigongbao.module.basic.device.enums;

import lombok.Getter;

@Getter
public enum DeviceTypeEnum {
    PRINTER_SLA("PRINTER_SLA", "光固化3D打印机", true),
    WASH_CONTAINER("WASH_CONTAINER", "酒精容器", false),
    UV_CURING("UV_CURING", "UV固化机", false),
    ULTRASONIC_CLEANER("ULTRASONIC_CLEANER", "超声清洗机", false),
    AIR_COMPRESSOR("AIR_COMPRESSOR", "空气压缩机", false),
    DRYER("DRYER", "烘干设备", false),
    SEALING_MACHINE("SEALING_MACHINE", "封口机", false);

    private final String code;
    private final String name;
    /** 是否由 WebSocket 自动注册（true=禁止手动创建） */
    private final boolean autoRegistered;

    DeviceTypeEnum(String code, String name, boolean autoRegistered) {
        this.code = code;
        this.name = name;
        this.autoRegistered = autoRegistered;
    }
}
