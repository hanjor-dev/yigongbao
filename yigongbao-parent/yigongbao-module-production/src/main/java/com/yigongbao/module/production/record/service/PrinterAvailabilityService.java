package com.yigongbao.module.production.record.service;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.PrinterDeviceStateEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.production.record.vo.PrinterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PrinterAvailabilityService {

    private final PrinterDeviceUsageChecker usageChecker;

    public boolean isAvailable(DeviceEntity device, boolean activeUsage) {
        return device != null
                && Integer.valueOf(1).equals(device.getConnectionStatus())
                && Integer.valueOf(0).equals(device.getState())
                && !activeUsage;
    }

    public void requireAvailable(DeviceEntity device, boolean activeUsage) {
        if (!isAvailable(device, activeUsage)) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
    }

    public List<PrinterVO> toPrinterVOs(Collection<DeviceEntity> devices) {
        List<DeviceEntity> deviceList = devices == null
                ? List.of()
                : devices.stream().filter(Objects::nonNull).toList();
        List<Long> deviceIds = deviceList.stream().map(DeviceEntity::getId).toList();
        Set<Long> activeDeviceIds = Objects.requireNonNull(
                usageChecker.findActiveDeviceIds(deviceIds),
                "findActiveDeviceIds must not return null");
        return deviceList.stream()
                .map(device -> toPrinterVO(device, activeDeviceIds.contains(device.getId())))
                .toList();
    }

    private PrinterVO toPrinterVO(DeviceEntity device, boolean activeUsage) {
        boolean available = isAvailable(device, activeUsage);
        PrinterDeviceStateEnum deviceState = PrinterDeviceStateEnum.fromCode(device.getState());
        PrinterVO vo = new PrinterVO();
        vo.setId(device.getId());
        vo.setDeviceNo(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setStatus(available ? 0 : 1);
        vo.setStatusName(available ? "空闲" : "占用");
        vo.setDeviceState(device.getState());
        vo.setDeviceStateName(deviceState != null ? deviceState.getName() : null);
        vo.setConnectionStatus(device.getConnectionStatus());
        vo.setAvailable(available);
        return vo;
    }
}
