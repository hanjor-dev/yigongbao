package com.yigongbao.module.basic.device.task;

import com.yigongbao.module.basic.device.service.IDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOfflineDetectionTask {

    private final IDeviceService deviceService;

    @Scheduled(cron = "0 * * * * ?")
    public void detectOfflineDevices() {
        log.debug("开始执行设备离线检测任务");
        try {
            deviceService.detectOfflineDevices();
        } catch (Exception e) {
            log.error("设备离线检测任务执行失败", e);
        }
    }
}
