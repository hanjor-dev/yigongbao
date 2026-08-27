package com.yigongbao.module.order.vo.order;

import com.yigongbao.common.vo.StatusColorVO;
import com.yigongbao.module.order.vo.AuditInfo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项 VO
 * 用于订单列表分页查询返回的数据结构
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 订单类型：1-医疗器械，2-非医疗器械
     */
    private Integer orderType;

    /**
     * 订单类型名称（补充显示）
     */
    private String orderTypeName;

    /**
     * 是否需要实体交付：0-不需要，1-需要，2-异地打印
     */
    private Integer needsPhysicalDelivery;

    /**
     * 是否需要实体交付名称（补充显示）
     */
    private String needsPhysicalDeliveryName;

    /**
     * 业务类型（字典 dict_code）
     */
    private String businessType;

    /**
     * 业务类型名称（翻译后展示）
     */
    private String businessTypeName;

    // ==================== 机构信息 ====================

    /**
     * 提单机构ID
     */
    private Long orgId;

    /**
     * 提单机构名称
     */
    private String orgName;

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员姓名
     */
    private String operatorName;

    /**
     * 操作员电话
     */
    private String operatorPhone;

    /**
     * 提单人所属部门ID
     */
    private Long operatorDeptId;

    /**
     * 提单人所属部门名称
     */
    private String operatorDeptName;

    // ==================== 医院与地区 ====================

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称（冗余）
     */
    private String hospitalName;

    /**
     * 地区ID
     */
    private Long areaId;

    /**
     * 地区名称
     */
    private String areaName;

    /**
     * 完整地区路径名称
     */
    private String fullAreaName;

    /**
     * 医院科室ID
     */
    private Long hospitalDeptId;

    /**
     * 医院科室名称
     */
    private String hospitalDeptName;

    // ==================== 科室与医生 ====================

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
     * 患者性别（字典值）
     */
    private String patientGender;

    /**
     * 患者性别名称
     */
    private String patientGenderName;

    // ==================== 业务标识 ====================

    /**
     * 是否为经典案例：0-否，1-是
     */
    private Integer isClassicCase;

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

    // ==================== 处理人与时效 ====================

    /**
     * 设计师ID
     */
    private Long designerId;

    /**
     * 设计师姓名
     */
    private String designerName;

    /**
     * 期望交付时间
     */
    private LocalDateTime expectedDeliveryDate;

    /**
     * 预估费用
     */
    private BigDecimal estimatedCost;

    /**
     * 影像数据评估意见
     */
    private String dataEvaluationOpinion;

    // ==================== 阶段与状态 ====================

    /**
     * 当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成
     */
    private Integer phase;

    /**
     * 当前阶段名称
     */
    private String phaseName;

    /**
     * 当前状态
     */
    private Integer status;

    /**
     * 当前状态名称
     */
    private String statusName;

    /** 当前状态标签颜色 */
    private StatusColorVO statusColor;

    // ==================== 审核信息 ====================

    /**
     * 历史区域审核信息（兼容旧试用订单，新订单不再产生）
     */
    private AuditInfo regionalAudit;

    /**
     * 设计审核信息
     */
    private AuditInfo designAudit;

    /** 是否允许当前用户打开订单修改页面 */

    // ==================== 时间 ====================

    /**
     * 设计开始时间
     */
    private LocalDateTime designStartTime;

    /**
     * 设计提交时间
     */
    private LocalDateTime designSubmitTime;

    /**
     * 生产开始时间
     */
    private LocalDateTime productionStartTime;

    /**
     * 生产结束时间
     */
    private LocalDateTime productionEndTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ==================== 版本控制 ====================

    /**
     * 版本号（乐观锁）
     */
    private Integer version;

    // ==================== 重建项目列表 ====================

    /**
     * 重建项目明细列表（按需填充，非列表查询默认字段）
     */
    private List<RebuildProjectItemVO> rebuildProjectList;

    /**
     * 重建项目明细 VO（内嵌类）
     */
    @Data
    public static class RebuildProjectItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 重建部位名称
         */
        private String bodyPartName;

        /**
         * 重建项目名称
         */
        private String projectName;

        /**
         * 项目分类编码（字典 dict_code=13）
         */
        private String categoryCode;

        /**
         * 项目分类名称
         */
        private String categoryName;

        /**
         * 数量（明细表粒度为每个部位一条，固定为 1）
         */
        private Integer count;

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
    }
}
