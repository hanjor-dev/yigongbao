package com.yigongbao.module.production.qc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 质检产品记录实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process_product_result")
public class ProductionProcessProductResultEntity extends BaseEntity {
    private Long productionProcessId;
    private Long productionProductId;
    private String result;
    private String remark;
    private Integer attemptNo;
    private Integer isLatest;
    private Long inspectorId;
    private LocalDateTime inspectTime;
}
