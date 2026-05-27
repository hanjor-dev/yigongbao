package com.yigongbao.module.production.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 工序流转记录实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process_transfer")
public class ProductionProcessTransferEntity extends BaseEntity {
    private Long productionRecordId;
    private String fromProcessType;
    private String toProcessType;
    private LocalDateTime transferTime;
    private Long scanUserId;
    private String scanUserName;
    private Long handoverUserId;
    private String handoverUserName;
    private String remark;
}
