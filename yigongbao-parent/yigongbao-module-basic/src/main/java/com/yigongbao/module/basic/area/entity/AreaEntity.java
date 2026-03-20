package com.yigongbao.module.basic.area.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 地区 Entity
 * 与 https://github.com/kakuilan/china_area_mysql cnarea_2023 表结构完全一致
 * 不继承 BaseEntity，不含项目公共字段
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
@TableName("sys_area")
public class AreaEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 层级（1=省/直辖市，2=市，3=区/县）
     */
    private Integer level;

    /**
     * 父级行政代码
     */
    @TableField("parent_code")
    private Long parentCode;

    /**
     * 行政代码（国家标准）
     */
    @TableField("area_code")
    private Long areaCode;

    /**
     * 邮政编码
     */
    @TableField("zip_code")
    private Integer zipCode;

    /**
     * 区号
     */
    @TableField("city_code")
    private String cityCode;

    /**
     * 名称
     */
    private String name;

    /**
     * 简称
     */
    @TableField("short_name")
    private String shortName;

    /**
     * 组合名
     */
    @TableField("merger_name")
    private String mergerName;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 经度
     */
    private BigDecimal lng;

    /**
     * 纬度
     */
    private BigDecimal lat;
}
