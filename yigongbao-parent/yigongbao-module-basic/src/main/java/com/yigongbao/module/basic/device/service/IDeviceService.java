package com.yigongbao.module.basic.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.dto.UpdateDeviceDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.module.basic.device.vo.DeviceStatisticsVO;
import java.util.List;

public interface IDeviceService extends IService<DeviceEntity> {
    IPage<DeviceVO> listDevices(DevicePageDTO dto);
    DeviceStatisticsVO getStatistics(DevicePageDTO dto);
    DeviceVO getDeviceById(Long id);
    Long createDevice(CreateDeviceDTO dto);
    void updateDeviceState(Long id, Integer state);
    List<DeviceVO> listDevicesByCenterAndType(Long centerId, String deviceType);
    boolean batchUpdateDeviceStatus(DeviceStatusPushDTO dto);
    void markDevicesOffline(Long centerId);
    void updateDevice(UpdateDeviceDTO dto);
    void removeDevice(Long id);
}
