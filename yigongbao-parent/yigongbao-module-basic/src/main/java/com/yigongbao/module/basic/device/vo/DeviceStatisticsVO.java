package com.yigongbao.module.basic.device.vo;

import lombok.Data;

@Data
public class DeviceStatisticsVO {
    /** 统计范围内的设备总数。 */
    private Long total = 0L;

    /** state=0 的设备数。 */
    private Long idle = 0L;

    /** state 非 0 的设备数。 */
    private Long occupied = 0L;
}
