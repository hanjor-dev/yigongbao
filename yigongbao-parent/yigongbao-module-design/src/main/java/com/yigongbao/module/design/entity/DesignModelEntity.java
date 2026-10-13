package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可视化模型文件 Entity
 * 文件详情通过 fileId 关联 file_detail 表查询
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_model")
@EqualsAndHashCode(callSuper = false)
public class DesignModelEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 文件ID（关联 file_detail.id）
     */
    private String fileId;
}
