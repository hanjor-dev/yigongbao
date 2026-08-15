package com.yigongbao.module.production.record.service;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.PrinterDeviceStateEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.production.record.vo.PrinterVO;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class PrinterAvailabilityService {

    public boolean isAvailable(DeviceEntity device) {
        return device != null
                && DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())
                && Integer.valueOf(1).equals(device.getConnectionStatus())
                && PrinterDeviceStateEnum.IDLE.getCode().equals(device.getState());
    }

    public void requireAvailable(DeviceEntity device) {
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
        if (!DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_TYPE_MISMATCH);
        }
        if (!isAvailable(device)) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
    }

    public List<PrinterVO> toPrinterVOs(Collection<DeviceEntity> devices) {
        List<DeviceEntity> deviceList = devices == null
                ? List.of()
                : devices.stream().filter(Objects::nonNull).toList();
        return deviceList.stream()
                .map(this::toPrinterVO)
                .toList();
    }

    private PrinterVO toPrinterVO(DeviceEntity device) {
        boolean available = isAvailable(device);
        PrinterDeviceStateEnum deviceState = PrinterDeviceStateEnum.fromCode(device.getState());
        PrinterVO vo = new PrinterVO();
        vo.setId(device.getId());
        vo.setDeviceNo(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setStatus(available ? 0 : 1);
        vo.setStatusName(available ? "空闲" : "不可用");
        vo.setDeviceState(device.getState());
        vo.setDeviceStateName(deviceState != null ? deviceState.getName() : null);
        vo.setConnectionStatus(device.getConnectionStatus());
        vo.setAvailable(available);
        return vo;
    }
}
