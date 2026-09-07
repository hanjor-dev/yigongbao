package com.yigongbao.module.design.vo;

import com.yigongbao.common.vo.StatusColorVO;
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
    /** 订单ID */
    private Long id;
    /** 订单编号 */
    private String orderCode;
    /** 订单状态码 */
    private Integer status;
    /** 订单状态名称 */
    private String statusName;

    /** 当前状态标签颜色 */
    private StatusColorVO statusColor;
    /** 阶段码 */
    private Integer phase;
    /** 阶段名称 */
    private String phaseName;
    /** 设计模式：1=线下修改，2=在线编辑 */
    private Integer designMode;
    /** 最近一次驳回原因 */
    private String rejectReason;

    // ==================== 订单类型 ====================
    /** 订单类型：1=直提，2=草稿提交 */
    private Integer orderType;
    /** 订单类型名称 */
    private String orderTypeName;
    /** 是否需要实物交付：0=否，1=是 */
    private Integer needsPhysicalDelivery;
    /** 是否需要实物交付名称 */
    private String needsPhysicalDeliveryName;
    /** 业务类型 dict_code（如 12.1） */
    private String businessType;
    /** 业务类型名称 */
    private String businessTypeName;

    // ==================== 机构信息 ====================
    /** 提单机构ID */
    private Long orgId;
    /** 提单机构名称 */
    private String orgName;
    /** 提单人（操作员）ID */
    private Long operatorId;
    /** 提单人姓名 */
    private String operatorName;
    /** 提单人联系电话 */
    private String operatorPhone;

    // ==================== 医院信息 ====================
    /** 医院ID */
    private Long hospitalId;
    /** 医院名称 */
    private String hospitalName;
    /** 医院科室名称 */
    private String hospitalDeptName;
    /** 地区简称（如"北京"） */
    private String areaName;
    /** 地区全称（如"北京市/海淀区"） */
    private String fullAreaName;

    // ==================== 医生/患者信息 ====================
    /** 医生姓名 */
    private String doctorName;
    /** 医生联系电话 */
    private String doctorPhone;
    /** 患者姓名 */
    private String patientName;
    /** 患者年龄 */
    private Integer patientAge;
    /** 患者性别 dict_code（如 3.1=男） */
    private String patientGender;
    /** 患者性别名称 */
    private String patientGenderName;

    // ==================== 业务信息 ====================
    /** 是否加急：0=普通，1=加急 */
    private Integer isUrgent;
    /** 是否邮寄：0=否，1=是 */
    private Integer isPostal;
    /** 邮寄地址 */
    private String postalAddress;
    /** 预交货时间 */
    private LocalDateTime expectedDeliveryDate;

    // ==================== 设计信息 ====================
    /** 分配设计师ID */
    private Long designerId;
    /** 分配设计师姓名 */
    private String designerName;
    /** 设计开始时间 */
    private LocalDateTime designStartTime;
    /** 设计提交时间 */
    private LocalDateTime designSubmitTime;

    /** 设计师备注 */
    private String designerRemark;

    /** 版本号（乐观锁） */
    private Integer version;

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
        /** 重建项目名称 */
        private String projectName;
        /** 部位名称 */
        private String bodyPartName;
        /** 项目分类编码（如 13.1=模型） */
        private String categoryCode;
        /** 项目分类名称 */
        private String categoryName;
        /** 数量（固定为1，预留扩展） */
        private Integer count;
        /** 项目说明（用户下单时填写，可覆盖重建项目默认描述） */
        private String projectDesc;
        /** 成型需求（用户下单时填写） */
        private String formingRequirement;
        /** 其他要求（用户下单时填写） */
        private String otherRequirement;
    }
}
