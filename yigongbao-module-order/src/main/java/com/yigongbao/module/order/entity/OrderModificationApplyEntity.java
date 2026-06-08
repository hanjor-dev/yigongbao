package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单修改申请实体
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_modification_apply")
public class OrderModificationApplyEntity extends BaseEntity {

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 订单编号（冗余）
     */
    @TableField("order_code")
    private String orderCode;

    /**
     * 申请类型：FULL=全量修改
     */
    @TableField("apply_type")
    private Integer applyType;

    /**
     * 修改内容（完整OrderModifyFullDTO的JSON，用于审核通过后执行）
     */
    @TableField("modification_content")
    private String modificationContent;

    /**
     * 变更差异（结构化差异JSON，用于审核界面展示）
     */
    @TableField("modification_diff")
    private String modificationDiff;

    /**
     * 申请人ID
     */
    @TableField("apply_user_id")
    private Long applyUserId;

    /**
     * 申请人姓名（冗余）
     */
    @TableField("apply_user_name")
    private String applyUserName;

    /**
     * 申请时间
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 过期时间（申请时间 + 10分钟）
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 状态：0=待审核，1=已通过，2=已驳回，3=已过期
     */
    @TableField("status")
    private Integer status;

    /**
     * 审核人ID
     */
    @TableField("audit_user_id")
    private Long auditUserId;

    /**
     * 审核人姓名（冗余）
     */
    @TableField("audit_user_name")
    private String auditUserName;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核备注（驳回原因）
     */
    @TableField("audit_remark")
    private String auditRemark;
}
