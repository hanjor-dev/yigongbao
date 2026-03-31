package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 订单文件关联表 Entity
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
@TableName("order_file")
@EqualsAndHashCode(callSuper = false)
public class OrderFileEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与关联字段 ====================
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderCode;

    // ==================== 文件关联 ====================
    /**
     * 文件ID（file_detail.id）
     */
    private String fileId;

    /**
     * 文件类别（字典 dict_code：10.1-影像数据，10.2-影像报告...）
     */
    private String fileCategory;

    /**
     * 数据包编号
     */
    private String packageNo;

    // ==================== 关联明细 ====================
    /**
     * 关联的订单明细ID
     */
    private Long orderItemId;
}
