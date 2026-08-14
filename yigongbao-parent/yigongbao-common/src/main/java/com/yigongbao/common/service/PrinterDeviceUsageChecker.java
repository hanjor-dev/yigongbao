package com.yigongbao.common.service;

import java.util.Collection;
import java.util.List;
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

    default boolean isInUse(Long deviceId) {
        if (deviceId == null) {
            return false;
        }
        Set<Long> activeDeviceIds = findActiveDeviceIds(List.of(deviceId));
        return activeDeviceIds != null && activeDeviceIds.contains(deviceId);
    }
}
