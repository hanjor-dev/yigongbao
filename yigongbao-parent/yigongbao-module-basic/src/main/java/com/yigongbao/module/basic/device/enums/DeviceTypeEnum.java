package com.yigongbao.module.basic.device.enums;

import lombok.Getter;

@Getter
public enum DeviceTypeEnum {
    PRINTER_SLA("PRINTER_SLA", "光固化3D打印机"),
    WASH_CONTAINER("WASH_CONTAINER", "酒精容器"),
    UV_CURING("UV_CURING", "UV固化机"),
    ULTRASONIC_CLEANER("ULTRASONIC_CLEANER", "超声清洗机"),
    AIR_COMPRESSOR("AIR_COMPRESSOR", "空气压缩机"),
    DRYER("DRYER", "烘干设备"),
    SEALING_MACHINE("SEALING_MACHINE", "封口机");

    private final String code;
    private final String name;

    DeviceTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
