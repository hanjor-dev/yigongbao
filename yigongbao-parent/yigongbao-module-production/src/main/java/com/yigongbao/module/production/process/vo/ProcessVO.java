package com.yigongbao.module.production.process.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工序信息 VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProcessVO {
    private Long id;
    /** 工序类型代码（print/wash/cure/clean_dry/pack） */
    private String processType;
    /** 工序名称 */
    private String processName;
    /** 工序执行顺序 */
    private Integer processOrder;
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
    /** 工序结束时间 */
    private LocalDateTime endTime;
    /** 操作员姓名 */
    private String operatorName;
    /** 工序状态（pending/in_progress/completed） */
    private String status;
    /** 辅助设备ID（clean_dry 工序的干燥设备） */
    private Long secondaryDeviceId;
    /** 辅助设备编号 */
    private String secondaryDeviceNo;
    /** 辅助设备名称 */
    private String secondaryDeviceName;
    /** 是否有重做（0=否，1=是） */
    private Integer hasRedo;
    /** 重做备注 */
    private String redoRemark;
    /** 检验结果 */
    private String inspectionResult;
    /** 检验员姓名 */
    private String inspectorName;
}
