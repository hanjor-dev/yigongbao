package com.yigongbao.module.basic.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import com.yigongbao.module.basic.device.mapper.DeviceStateLogMapper;
import com.yigongbao.module.basic.device.service.IDeviceStateLogService;
import org.springframework.stereotype.Service;

@Service
public class DeviceStateLogServiceImpl extends ServiceImpl<DeviceStateLogMapper, DeviceStateLogEntity>
        implements IDeviceStateLogService {
}
