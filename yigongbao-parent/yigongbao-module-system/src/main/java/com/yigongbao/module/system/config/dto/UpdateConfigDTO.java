package com.yigongbao.module.system.config.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新配置 DTO
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Data
public class UpdateConfigDTO {

    /**
     * 配置名称
     */
    @Size(max = 128, message = "配置名称长度不能超过128个字符")
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
     * 配置分组（system/security/other）
     */
    private String configGroup;

    /**
     * 配置说明
     */
    @Size(max = 256, message = "配置说明长度不能超过256个字符")
    private String configDesc;

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
}
