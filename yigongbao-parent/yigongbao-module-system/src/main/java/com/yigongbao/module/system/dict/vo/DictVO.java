package com.yigongbao.module.system.dict.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典 VO（视图对象）
 * 用于返回给前端的字典数据
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class DictVO {

    /**
     * 主键ID
     */
    private Long id;

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子节点列表（用于树形结构）
     */
    private List<DictVO> children;
}
