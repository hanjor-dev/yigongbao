package com.yigongbao.module.system.hospitalGroupTemplate.entity;

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

    private String templateName;
    private String templateCode;
    private String templateDesc;
    private Integer status;
    private String remark;
}
