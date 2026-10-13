package com.yigongbao.module.basic.chargingTemplate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 收费模板 Entity
 * 用于管理不同业务账号的收费标准模板
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
@TableName("charging_template")
@EqualsAndHashCode(callSuper = false)
public class ChargingTemplateEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
