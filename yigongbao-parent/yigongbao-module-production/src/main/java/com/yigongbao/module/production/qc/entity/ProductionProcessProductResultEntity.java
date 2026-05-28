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
    /** 所属工序记录ID */
    private Long productionProcessId;
    /** 被检验的产品ID */
    private Long productionProductId;
    /** 检验结果（pass/redo） */
    private String result;
    /** 不合格原因 */
    private String remark;
    /** 本产品第几次检验（从1开始） */
    private Integer attemptNo;
    /** 是否为最新一次检验记录（0=否，1=是） */
    private Integer isLatest;
    /** 检验员ID */
    private Long inspectorId;
    /** 检验时间 */
    private LocalDateTime inspectTime;
}
