package com.yigongbao.module.basic.bodyPart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 重建部位 Entity
 * 平级结构，部位直接关联重建项目
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
@TableName("rebuild_body_part")
@EqualsAndHashCode(callSuper = false)
public class BodyPartEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位名称
     */
    private String name;

    /**
     * 部位编码
     */
    private String code;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
