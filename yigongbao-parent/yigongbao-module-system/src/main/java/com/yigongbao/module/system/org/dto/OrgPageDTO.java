package com.yigongbao.module.system.org.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 机构分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class OrgPageDTO implements Serializable {

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
     * 机构名称（模糊查询）
     */
    private String orgName;

    /**
     * 机构类型（字典编码）
     */
    private String orgType;

    /**
     * 地区ID
     */
    private Long areaId;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
