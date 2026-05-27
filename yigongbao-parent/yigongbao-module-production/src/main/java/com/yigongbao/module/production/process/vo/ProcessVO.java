package com.yigongbao.module.production.process.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工序信息VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProcessVO {
    private Long id;
    private String processType;
    private String processName;
    private Integer processOrder;
    private Long deviceId;
    private String deviceNo;
    private String deviceName;
    private String processParams;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operatorName;
    private String status;
}
