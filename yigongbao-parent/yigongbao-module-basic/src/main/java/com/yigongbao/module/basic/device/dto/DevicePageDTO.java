package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 设备分页查询请求 DTO
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class DevicePageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认10条 */
    private Integer pageSize = 10;

    /** 所属加工中心ID（精确匹配） */
    private Long centerId;

    /** 设备类型（精确匹配，如 PRINTER_SLA） */
    private String deviceType;

    /** 设备状态（0=空闲，1=占用） */
    private Integer state;

    /** 连接状态（0=离线，1=在线） */
    private Integer connectionStatus;

    /** 设备编号（模糊查询） */
    private String deviceId;
}
