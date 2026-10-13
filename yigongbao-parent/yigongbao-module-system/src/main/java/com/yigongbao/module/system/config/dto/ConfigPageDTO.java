package com.yigongbao.module.system.config.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class ConfigPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 配置键（模糊查询）
     */
    private String configKey;

    /**
     * 配置名称（模糊查询）
     */
    private String configName;

    /**
     * 配置分组（精确查询）
     */
    private String configGroup;

    /**
     * 配置类型（精确查询）
     */
    private String configType;

    /**
     * 状态（精确查询）
     */
    private Integer status;
}
