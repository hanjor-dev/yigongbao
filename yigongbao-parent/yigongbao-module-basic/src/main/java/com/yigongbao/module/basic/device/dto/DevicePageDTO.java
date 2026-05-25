package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class DevicePageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long centerId;
    private String deviceType;
    private Integer state;
    private Integer connectionStatus;
    private String deviceId;
}
