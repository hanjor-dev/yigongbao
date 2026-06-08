package com.yigongbao.module.basic.chargingTemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 收费模板明细 Entity
 * 存储每个收费模板中各重建项目的价格
 * 注意：此表使用物理删除，不继承 BaseEntity 的 isDeleted 字段
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("charging_template_item")
public class ChargingTemplateItemEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID（关联charging_template表）
     */
    private Long templateId;

    /**
     * 重建项目ID（关联rebuild_project表）
     */
    private Long rebuildProjectId;

    /**
     * 收费价格（元）
     */
    private BigDecimal price;

    /**
     * 逻辑删除字段（表中不存在，仅用于覆盖父类字段）
     */
    @TableField(exist = false)
    private Integer isDeleted;
}
