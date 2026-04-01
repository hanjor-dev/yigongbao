package com.yigongbao.module.order.vo.draft;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 草稿详情 VO
 * 用于草稿详情查询返回的完整数据结构
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderDraftDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与操作人 ====================
    /**
     * 草稿ID
     */
    private Long id;

    /**
     * 操作员ID（创建人）
     */
    private Long operatorId;

    /**
     * 操作员姓名（冗余）
     */
    private String operatorName;

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

    /**
     * 业务类型名称（补充显示）
     */
    private String businessTypeName;

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

    // ==================== 有效期管理 ====================
    /**
     * 过期时间（创建时间+30天）
     */
    private LocalDateTime expiresAt;

    /**
     * 状态：1-有效，2-已提交，3-已过期
     */
    private Integer status;

    /**
     * 状态名称（补充显示）
     */
    private String statusName;

    // ==================== 重建项目列表 ====================
    /**
     * 重建项目明细列表
     */
    private List<OrderItemDraftVO> items;

    /**
     * 重建项目数量
     */
    private Integer itemCount;

    // ==================== 时间信息 ====================
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ==================== 重建项目明细 VO ====================
    /**
     * 重建项目明细 VO
     * 嵌套在 OrderDraftDetailVO 中
     *
     * @author hanjor
     * @date 2026-03-31
     */
    @Data
    public static class OrderItemDraftVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 草稿明细ID
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

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }
}
