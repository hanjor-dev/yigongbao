package com.yigongbao.common.service;

/**
 * 支持排除指定流转卡的打印设备生产占用查询端口。
 */
public interface PrinterRecordUsageChecker extends PrinterDeviceUsageChecker {

    /**
     * 查询设备是否被除指定流转卡以外的活跃生产记录占用。
     *
     * @param deviceId 设备 ID；为 {@code null} 时返回 {@code false}
     * @param excludedRecordId 需排除的流转卡记录 ID，可为 {@code null}
     * @return 是否存在其他活跃生产记录
     */
    boolean isInUseByOtherRecord(Long deviceId, Long excludedRecordId);
}
