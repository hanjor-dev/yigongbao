package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单修改申请表
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
@TableName("order_modification_apply")
public class OrderModificationApplyEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_code")
    private String orderCode;

    @TableField("apply_type")
    private Integer applyType;

    @TableField("modification_content")
    private String modificationContent;

    @TableField("modification_diff")
    private String modificationDiff;

    @TableField("apply_user_id")
    private Long applyUserId;

    @TableField("apply_user_name")
    private String applyUserName;

    @TableField("apply_time")
    private LocalDateTime applyTime;

    @TableField("apply_phase")
    private Integer applyPhase;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_user_name")
    private String auditUserName;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("is_deleted")
    private Integer isDeleted;
}
