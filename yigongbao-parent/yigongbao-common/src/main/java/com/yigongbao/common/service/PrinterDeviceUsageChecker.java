package com.yigongbao.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 打印设备生产占用查询端口。
 */
public interface PrinterDeviceUsageChecker {

    /**
     * 查找正在被生产占用的设备 ID。
     * <p>
     * 实现必须在输入为 {@code null} 或空集合时返回空集合，忽略输入中的
     * {@code null} 元素，并且始终返回非 {@code null} 集合。
     *
     * @param deviceIds 待查询的设备 ID
     * @return 正在被生产占用的设备 ID 集合，永不为 {@code null}
     */
    Set<Long> findActiveDeviceIds(Collection<Long> deviceIds);

    /**
     * 查询设备是否被除指定流转卡以外的活跃生产记录占用。
     *
     * @param deviceId 设备 ID
     * @param excludedRecordId 需排除的流转卡记录 ID，可为 {@code null}
     * @return 是否存在其他活跃生产记录
     */
    default boolean isInUseByOtherRecord(Long deviceId, Long excludedRecordId) {
        throw new UnsupportedOperationException("Query by excluded record is not supported");
    }

    default boolean isInUse(Long deviceId) {
        if (deviceId == null) {
            return false;
        }
        Set<Long> activeDeviceIds = Objects.requireNonNull(
                findActiveDeviceIds(List.of(deviceId)),
                "findActiveDeviceIds must not return null"
        );
        return activeDeviceIds.contains(deviceId);
    }
}
