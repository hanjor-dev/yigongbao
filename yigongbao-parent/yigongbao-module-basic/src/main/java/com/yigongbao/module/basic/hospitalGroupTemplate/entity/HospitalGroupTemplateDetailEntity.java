package com.yigongbao.module.basic.hospitalGroupTemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院组合模板明细 Entity
 * 不继承 BaseEntity，极简设计，仅记录关联关系
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("hospital_group_template_detail")
public class HospitalGroupTemplateDetailEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
