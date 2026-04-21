package com.yigongbao.module.design.vo;

import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设计工单详情 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderDetailVO {

    // ==================== 订单基本信息 ====================
    private Long id;
    private String orderCode;
    private Integer status;
    private String statusName;
    private Integer phase;
    private String phaseName;
    /** 设计模式：1=线下修改，2=在线编辑 */
    private Integer designMode;
    /** 最近一次驳回原因 */
    private String rejectReason;

    // ==================== 订单类型 ====================
    private Integer orderType;
    private String orderTypeName;
    private Integer needsPhysicalDelivery;
    private String needsPhysicalDeliveryName;
    private String businessType;
    private String businessTypeName;

    // ==================== 机构信息 ====================
    private Long orgId;
    private String orgName;
    private Long operatorId;
    private String operatorName;
    private String operatorPhone;

    // ==================== 医院信息 ====================
    private Long hospitalId;
    private String hospitalName;
    private String hospitalDeptName;
    private String areaName;
    private String fullAreaName;

    // ==================== 医生/患者信息 ====================
    private String doctorName;
    private String doctorPhone;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientGenderName;

    // ==================== 业务信息 ====================
    private Integer isUrgent;
    private Integer isPostal;
    private String postalAddress;
    private LocalDateTime expectedDeliveryDate;

    // ==================== 设计信息 ====================
    private Long designerId;
    private String designerName;
    private LocalDateTime designStartTime;
    private LocalDateTime designSubmitTime;

    // ==================== 重建项目列表 ====================
    private List<RebuildProjectItemVO> rebuildProjectList;

    // ==================== 提交校验状态 ====================
    private SubmitCheckVO submitCheck;

    // ==================== 订单影像文件（订单阶段上传） ====================
    /** 影像数据文件列表（CT/MRI等，fileCategory=10.1） */
    private List<OrderDetailVO.OrderFileVO> imageDataFiles;

    /** 影像报告文件列表（fileCategory=10.2） */
    private List<OrderDetailVO.OrderFileVO> imageReportFiles;

    // ==================== 设计阶段文件 ====================
    /** 打印文件数据包列表（含包内文件及最新版指令单/图纸） */
    private List<DesignPackageVO> packageList;

    /** 可视化模型列表 */
    private List<DesignModelVO> modelList;

    /** 设计报告（无则为 null） */
    private FileVO report;

    @Data
    public static class RebuildProjectItemVO {
        private String projectName;
        private String bodyPartName;
        private String categoryCode;
        private String categoryName;
        private Integer count;
        private String projectDesc;
        private String formingRequirement;
        private String otherRequirement;
    }
}
