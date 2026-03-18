package com.yigongbao.module.system.config.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建配置 DTO
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Data
public class CreateConfigDTO {

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    @Size(max = 64, message = "配置键长度不能超过64个字符")
    private String configKey;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 128, message = "配置名称长度不能超过128个字符")
    private String configName;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型（string/number/boolean/json）
     */
    private String configType = "string";

    /**
     * 配置分组（system/security/other）
     */
    @NotBlank(message = "配置分组不能为空")
    private String configGroup;

    /**
     * 配置说明
     */
    @Size(max = 256, message = "配置说明长度不能超过256个字符")
    private String configDesc;

    /**
     * 是否系统内置（0=否，1=是）
     */
    private Integer isSystem = 0;

    /**
     * 是否公开（0=私密，1=公开）
     */
    private Integer isPublic = 1;

    /**
     * 排序
     */
    private Integer sort = 0;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status = 1;
}
