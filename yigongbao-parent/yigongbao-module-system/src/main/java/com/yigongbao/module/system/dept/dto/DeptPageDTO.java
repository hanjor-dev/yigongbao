package com.yigongbao.module.system.dept.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class DeptPageDTO implements Serializable {

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
     * 部门类型（1=内部，2=外部，可选过滤）
     */
    private Integer deptType;

    /**
     * 部门名称（模糊查询）
     */
    private String deptName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
