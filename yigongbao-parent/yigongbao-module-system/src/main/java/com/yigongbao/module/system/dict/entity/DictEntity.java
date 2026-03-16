package com.yigongbao.module.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 字典 Entity
 * 采用单表树形结构设计，通过 parent_id 实现层级关系
 * 编码规则：根节点=1/2/3，子节点=1.1/1.2，二级子节点=1.1.1/1.1.2
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
@TableName("sys_dict")
@EqualsAndHashCode(callSuper = false)
public class DictEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 父级ID（0表示根节点/字典类型）
     */
    private Long parentId;

    /**
     * 字典编码（层级数字，如：1、1.1、1.1.1）
     */
    private String dictCode;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典值（叶子节点使用）
     */
    private String dictValue;

    /**
     * 层级（1=字典类型，2=字典数据，3+=扩展层级）
     */
    private Integer level;

    /**
     * 排序（同级内排序）
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
