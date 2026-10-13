package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DesignPackageBatchVO {
    private Long batchId;
    private String batchNo;
    private Long orderId;
    private String status;
    private Integer sourceOrderStatus;
    private Integer packageCount;
    private Integer completedPackageCount;
    private Boolean canEdit;
    private LocalDateTime createTime;
    private LocalDateTime completedTime;
}
