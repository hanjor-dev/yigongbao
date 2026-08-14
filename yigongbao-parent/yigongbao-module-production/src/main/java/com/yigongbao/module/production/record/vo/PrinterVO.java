package com.yigongbao.module.production.record.vo;

import lombok.Data;

/**
 * 打印机VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class PrinterVO {
    private Long id;
    private String deviceNo;
    private String deviceName;
    /** 0=可用, 1=不可用 */
    private Integer status;
    private String statusName;
    private Integer deviceState;
    private String deviceStateName;
    private Integer connectionStatus;
    private Boolean available;
}
