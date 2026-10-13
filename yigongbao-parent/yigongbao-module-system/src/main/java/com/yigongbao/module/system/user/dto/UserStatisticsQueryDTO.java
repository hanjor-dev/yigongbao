package com.yigongbao.module.system.user.dto;

import lombok.Data;

/** 账户统计查询条件（不含账户类型条件）。 */
@Data
public class UserStatisticsQueryDTO {
    private String keyword;
    private Long roleId;
    private Long orgId;
    private Long deptId;
    private Integer status;
}
