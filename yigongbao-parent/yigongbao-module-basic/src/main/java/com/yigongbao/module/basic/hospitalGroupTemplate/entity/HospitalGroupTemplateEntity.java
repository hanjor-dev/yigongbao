package com.yigongbao.module.basic.hospitalGroupTemplate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 医院组合模板 Entity
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("hospital_group_template")
@EqualsAndHashCode(callSuper = false)
public class HospitalGroupTemplateEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码（系统唯一，自动生成）
     */
    private String templateCode;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
