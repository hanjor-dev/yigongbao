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
    /** 所属流转卡ID */
    private Long productionRecordId;
    /** 工序类型代码（print/wash/cure/clean_dry/pack） */
    private String processType;
    /** 工序名称 */
    private String processName;
    /** 工序执行顺序 */
    private Integer processOrder;
    /** 设备类型 */
    private String deviceType;
    /** 使用设备ID */
    private Long deviceId;
    /** 设备编号 */
    private String deviceNo;
    /** 设备名称 */
    private String deviceName;
    /** 工序参数（JSON格式） */
    private String processParams;
    /** 工序开始时间 */
    private LocalDateTime startTime;
    /** 工序结束时间（= 开始时间 + 设备配置耗时，未配置则为实际完成时间） */
    private LocalDateTime endTime;
    /** 操作员ID */
    private Long operatorId;
    /** 操作员姓名 */
    private String operatorName;
    /** 本工序是否有重做（0=否，1=是） */
    private Integer hasRedo;
    /** 重做备注 */
    private String redoRemark;
    /** 检验结果 */
    private String inspectionResult;
    /** 检验员ID */
    private Long inspectorId;
    /** 检验员姓名 */
    private String inspectorName;
    /** 工序状态（pending/in_progress/completed） */
    private String status;
}
