package com.yigongbao.module.basic.bodyPart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 重建部位 Entity
 * 管理身体部位树形结构（最多2级：身体区域 → 具体部位）
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
     * 父级ID（0=顶级身体区域）
     */
    private Long parentId;

    /**
     * 部位名称
     */
    private String name;

    /**
     * 部位编码
     */
    private String code;

    /**
     * 层级（1=身体区域，2=具体部位）
     */
    private Integer level;

    /**
     * 设计师编号（如A/B/C）
     */
    private String designerCode;

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
