package com.yigongbao.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 打印设备生产占用查询端口。
 */
public interface PrinterDeviceUsageChecker {

    Set<Long> findActiveDeviceIds(Collection<Long> deviceIds);

    default boolean isInUse(Long deviceId) {
        return deviceId != null && findActiveDeviceIds(List.of(deviceId)).contains(deviceId);
    }
}
