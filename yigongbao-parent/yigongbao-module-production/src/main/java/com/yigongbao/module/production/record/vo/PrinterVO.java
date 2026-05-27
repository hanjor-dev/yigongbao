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
    /** 0=离线, 1=可使用, 2=使用中 */
    private Integer status;
    private String statusName;
}
