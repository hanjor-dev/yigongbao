package com.yigongbao.module.system.basedata.area.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 地区 VO（视图对象）
 * 与 cnarea_2023 字段对应，支持树形结构
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class AreaVO {

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
    private Long parentCode;

    /**
     * 行政代码（国家标准）
     */
    private Long areaCode;

    /**
     * 邮政编码
     */
    private Integer zipCode;

    /**
     * 区号
     */
    private String cityCode;

    /**
     * 名称
     */
    private String name;

    /**
     * 简称
     */
    private String shortName;

    /**
     * 组合名
     */
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

    /**
     * 子节点列表（用于树形结构）
     */
    private List<AreaVO> children;
}
