package com.yigongbao.module.production.process.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 工序记录实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process")
public class ProductionProcessEntity extends BaseEntity {
    private Long productionRecordId;
    private String processType;
    private String processName;
    private Integer processOrder;
    private String deviceType;
    private Long deviceId;
    private String deviceNo;
    private String processParams;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long operatorId;
    private String operatorName;
    private Integer hasRedo;
    private String redoRemark;
    private String inspectionResult;
    private Long inspectorId;
    private String inspectorName;
    private String status;
}
