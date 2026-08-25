package com.yigongbao.module.design.vo;

import com.yigongbao.common.vo.StatusColorVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计工单列表项 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderListVO {

    private Long id;

    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;

    /** 订单编号 */
    private String orderCode;

    /** 当前状态值 */
    private Integer status;

    /** 当前状态名称 */
    private String statusName;

    /** 当前状态标签颜色 */
    private StatusColorVO statusColor;

    /** 业务类型字典码 */
    private String businessType;

    /** 业务类型名称 */
    private String businessTypeName;

    /** 订单类型（1=医疗器械，2=非医疗器械） */
    private Integer orderType;

    /** 订单类型名称 */
    private String orderTypeName;

    /** 是否需要实体交付（0=否，1=是，2=异地打印） */
    private Integer needsPhysicalDelivery;

    /** 实体交付名称 */
    private String needsPhysicalDeliveryName;

    /** 患者姓名 */
    private String patientName;

    /** 医院ID */
    private Long hospitalId;

    /** 医院名称 */
    private String hospitalName;

    /** 科室名称 */
    private String hospitalDeptName;

    /** 医生姓名 */
    private String doctorName;

    /** 地区名称 */
    private String areaName;

    /** 重建项目摘要，格式：左髋骨导板, 右髋骨模型 */
    private String rebuildProjectSummary;

    /** 设计师ID */
    private Long designerId;

    /** 设计师姓名 */
    private String designerName;

    /** 数据包数量 */
    private Integer packageCount;

    /** 开始设计时间 */
    private LocalDateTime designStartTime;

    /** 设计提交时间 */
    private LocalDateTime designSubmitTime;

    /** 期望交付日期 */
    private LocalDateTime expectedDeliveryDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 版本号（乐观锁） */
    private Integer version;

    /** 是否已填写打印信息（0=否，1=是） */
    private Integer hasPrintInfo;

    /** 驳回原因（最近一次，默认隐藏） */
    private String rejectReason;
}
