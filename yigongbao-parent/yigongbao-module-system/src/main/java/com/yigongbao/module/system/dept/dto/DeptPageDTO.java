package com.yigongbao.module.system.dept.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门分页查询请求参数
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class DeptPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认10条 */
    private Integer pageSize = 10;

    /** 部门类型（字典编码：6.1=企业部门，6.2=业务部门，可选过滤） */
    private String deptType;

    /** 部门名称（模糊查询） */
    private String deptName;

    /** 状态（0=禁用，1=正常） */
    private Integer status;
}
