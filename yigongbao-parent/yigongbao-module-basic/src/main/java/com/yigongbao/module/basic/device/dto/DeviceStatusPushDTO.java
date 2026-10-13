package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import java.util.List;

/**
 * WebSocket 设备状态推送 DTO
 * 由加工中心设备端通过 WebSocket 推送，包含该中心所有设备的当前状态
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class DeviceStatusPushDTO {

    /** 加工中心名称（用于匹配对应的加工中心记录） */
    private String centerName;

    /** 设备状态列表 */
    private List<DeviceStatus> devices;

    /**
     * 单个设备状态
     */
    @Data
    public static class DeviceStatus {

        /** 设备编号 */
        private String id;

        /** 设备状态（0=空闲，1=工作中，2=打印完成，3=报警，4=暂停，5=准备就绪，6=离线） */
        private Integer state;

        /** 打印开始时间（格式：yyyy-MM-dd HH:mm:ss） */
        private String printStartTime;

        /** 预计耗时（中文格式，例如：1天2小时3分钟） */
        private String estimatedDuration;
    }
}
