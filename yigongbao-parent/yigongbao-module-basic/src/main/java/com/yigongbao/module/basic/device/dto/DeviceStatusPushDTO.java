package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeviceStatusPushDTO {
    private String centerName;
    private List<DeviceStatus> devices;

    @Data
    public static class DeviceStatus {
        private String id;
        private Integer state;
    }
}
