package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 设计数据包追加批次。 */
@Data
@TableName("design_package_batch")
@EqualsAndHashCode(callSuper = false)
public class DesignPackageBatchEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String batchNo;
    private String batchType;
    private String status;
    private Integer sourceOrderStatus;
    private Long createdBy;
    private LocalDateTime completedTime;
    private Integer version;
}
