package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单修改留痕表 Entity
 * 永久审计日志，不继承 BaseEntity（无软删除、无 updateTime、无 updateBy）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
@TableName("order_modification_log")
public class OrderModificationLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    // ==================== 订单关联 ====================
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号（冗余）
     */
    private String orderCode;

    /**
     * 关联申请ID（走申请流程时有值，直接修改时为 null）
     */
    private Long applyId;

    // ==================== 字段变更信息 ====================
    /**
     * 修改字段名（驼峰，如 patientName）
     */
    private String fieldName;

    /**
     * 修改字段中文名（如 患者姓名）
     */
    private String fieldLabel;

    /**
     * 修改前值
     */
    private String oldValue;

    /**
     * 修改后值
     */
    private String newValue;

    // ==================== 操作人 ====================
    /**
     * 修改人ID
     */
    private Long modifierId;

    /**
     * 修改人姓名
     */
    private String modifierName;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
