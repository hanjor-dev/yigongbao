package com.yigongbao.module.system.dept.dto;

import lombok.Data;

/** 部门统计查询条件（不含部门类型条件）。 */
@Data
public class DeptStatisticsQueryDTO {
    private String deptName;
    private Integer status;
}
