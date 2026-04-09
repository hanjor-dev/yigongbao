package com.yigongbao.module.order.dto.modify;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行订单修改 DTO
 * 用于审核通过后的订单修改操作，专用于 OrderModifyApplyService.executeModification()
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ExecuteModificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息字段（14.1 类型）====================
    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院科室ID
     */
    private Long hospitalDeptId;

    /**
     * 医生ID（从历史联想列表选择时传入；与 doctorName 二选一）
     */
    private Long doctorId;

    /**
     * 医生姓名（手动输入时传入，触发快速创建/获取医生；与 doctorId 二选一）
     */
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 患者年龄
     */
    private Integer patientAge;

    /**
     * 患者性别（字典 dict_code：12.1-男，12.2-女）
     */
    private String patientGender;

    /**
     * 是否加急：0-否，1-是
     */
    private Integer isUrgent;

    /**
     * 是否邮寄：0-否，1-是
     */
    private Integer isPostal;

    /**
     * 邮寄地址
     */
    private String postalAddress;

    /**
     * 期望交付时间
     */
    private LocalDateTime expectedDeliveryDate;

    // ==================== 影像文件字段（14.2 类型）====================
    /**
     * 影像数据文件ID列表
     */
    private List<String> imageDataFileIds;

    /**
     * 影像报告文件ID列表
     */
    private List<String> imageReportFileIds;

    // ==================== 重建项目字段（14.3 类型）====================
    /**
     * 重建项目明细列表（增量差异：传 orderItemId 表示更新，不传表示新增；不在列表中的旧 item 将被删除）
     */
    private List<ExecuteModificationItemDTO> items;
}
