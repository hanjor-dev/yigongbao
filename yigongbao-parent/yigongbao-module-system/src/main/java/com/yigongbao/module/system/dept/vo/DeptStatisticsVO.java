package com.yigongbao.module.system.dept.vo;

import lombok.Data;

/** 部门类型统计结果。 */
@Data
public class DeptStatisticsVO {
    private Long total = 0L;
    private Long enterprise = 0L;
    private Long business = 0L;
}
