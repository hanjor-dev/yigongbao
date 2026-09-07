package com.yigongbao.module.system.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class UserPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认10条 */
    private Integer pageSize = 10;

    /** 用户名或真实姓名（模糊查询） */
    private String keyword;

    /** 所属机构ID */
    private Long orgId;

    /** 所属部门ID */
    private Long deptId;

    /** 账户分类（6.1=企业账户，6.2=业务账户） */
    private String accountType;

    /** 状态（0=禁用，1=正常） */
    private Integer status;
}
