package com.yigongbao.module.system.config.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Data
public class ConfigVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型（string/number/boolean/json）
     */
    private String configType;

    /**
     * 配置分组（system/security/file/other）
     */
    private String configGroup;

    /**
     * 配置说明
     */
    private String configDesc;

    /**
     * 是否系统内置（0=否，1=是）
     */
    private Integer isSystem;

    /**
     * 是否公开（0=私密，1=公开）
     */
    private Integer isPublic;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
