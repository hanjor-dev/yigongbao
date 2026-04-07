package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO
 * 用于订单详情查询返回的完整数据结构
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与编码 ====================
    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderCode;

    // ==================== 订单类型 ====================
    /**
     * 订单类型：1-医疗器械，2-非医疗器械
     */
    private Integer orderType;

    /**
     * 订单类型名称（补充显示）
     */
    private String orderTypeName;

    /**
     * 是否需要实体交付：0-不需要，1-需要
     */
    private Integer needsPhysicalDelivery;

    /**
     * 是否需要实体交付名称（补充显示）
     */
    private String needsPhysicalDeliveryName;

    /**
     * 业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）
     */
    private String businessType;

    // ==================== 机构信息 ====================
    /**
     * 提单机构ID
     */
    private Long orgId;

    /**
     * 提单机构名称（冗余）
     */
    private String orgName;

    /**
     * 操作员ID（创建人）
     */
    private Long operatorId;

    /**
     * 操作员姓名（冗余）
     */
    private String operatorName;

    /**
     * 操作员电话
     */
    private String operatorPhone;

    // ==================== 医院与科室 ====================
    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称（冗余）
     */
    private String hospitalName;

    /**
     * 地区ID（冗余自医院）
     */
    private Long areaId;

    /**
     * 地区名称（冗余自医院）
     */
    private String areaName;

    /**
     * 完整地区路径名称（冗余自医院，如"广东省/广州市/天河区"）
     */
    private String fullAreaName;

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 科室名称（冗余）
     */
    private String deptName;

    // ==================== 医生信息 ====================
    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    // ==================== 患者信息 ====================
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
     * 患者性别名称（补充显示）
     */
    private String patientGenderName;

    // ==================== 业务信息 ====================
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

    // ==================== 时效信息 ====================
    /**
     * 期望交付时间
     */
    private LocalDateTime expectedDeliveryDate;

    /**
     * 设计开始时间
     */
    private LocalDateTime designStartTime;

    /**
     * 设计提交时间
     */
    private LocalDateTime designSubmitTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime actualCompleteTime;

    // ==================== 阶段 + 状态 ====================
    /**
     * 当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成
     */
    private Integer phase;

    /**
     * 当前状态
     */
    private Integer status;

    // ==================== 当前处理人 ====================
    /**
     * 当前处理人ID
     */
    private Long currentHandlerId;

    /**
     * 当前处理人姓名
     */
    private String currentHandlerName;

    /**
     * 设计师ID
     */
    private Long designerId;

    /**
     * 设计师姓名（冗余）
     */
    private String designerName;

    /**
     * 生产员ID
     */
    private Long producerId;

    // ==================== 审核信息 ====================
    /**
     * 审核备注（驳回原因等）
     */
    private String auditRemark;

    /**
     * 设计审核备注
     */
    private String designReviewRemark;

    /**
     * 预估费用
     */
    private BigDecimal estimatedCost;

    /**
     * 影像数据评估意见
     */
    private String dataEvaluationOpinion;

    // ==================== 时间信息 ====================
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ==================== 订单明细 ====================
    /**
     * 订单明细列表
     */
    private List<OrderItemVO> items;

    /**
     * 订单明细数量
     */
    private Integer itemCount;

    /**
     * 可执行的动作列表
     */
    private List<String> availableActions;

    // ==================== 文件列表 ====================
    /**
     * 影像数据文件列表（dict_code=10.1，CT/MRI等）
     */
    private List<OrderFileVO> imageDataFiles;

    /**
     * 影像报告文件列表（dict_code=10.2）
     */
    private List<OrderFileVO> imageReportFiles;

    /**
     * 订单明细 VO
     * 嵌套在 OrderDetailVO 中
     *
     * @author hanjor
     * @date 2026-03-31
     */
    @Data
    public static class OrderItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 订单明细ID
         */
        private Long id;

        /**
         * 部位ID
         */
        private Long bodyPartId;

        /**
         * 部位名称
         */
        private String bodyPartName;

        /**
         * 重建项目ID
         */
        private Long projectId;

        /**
         * 重建项目名称
         */
        private String projectName;

        /**
         * 预计耗时（小时，支持小数）
         */
        private BigDecimal projectEstimatedHours;

        /**
         * 项目说明
         */
        private String projectDesc;

        /**
         * 成形需求
         */
        private String formingRequirement;

        /**
         * 其他要求
         */
        private String otherRequirement;

        /**
         * 排序序号
         */
        private Integer sortOrder;
    }

    /**
     * 订单文件 VO
     * 嵌套在 OrderDetailVO 中，用于展示订单关联的影像文件
     *
     * @author hanjor
     * @date 2026-04-02
     */
    @Data
    public static class OrderFileVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 文件ID
         */
        private String fileId;

        /**
         * 文件名称
         */
        private String fileName;

        /**
         * 文件类别（字典 dict_code：10.1-影像数据，10.2-影像报告）
         */
        private String fileCategory;

        /**
         * 文件类别名称
         */
        private String fileCategoryName;

        /**
         * 公开访问URL
         */
        private String fileUrl;

        /**
         * 缩略图访问URL
         */
        private String thUrl;

        /**
         * 文件大小（字节）
         */
        private Long fileSize;

        /**
         * 格式化文件大小（如 2.35 MB）
         */
        private String fileSizeText;

        /**
         * 文件扩展名
         */
        private String fileExt;
    }
}
