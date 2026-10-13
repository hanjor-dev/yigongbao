package com.yigongbao.module.system.org.dto;

import lombok.Data;

/** 机构统计查询条件（不含机构类型条件）。 */
@Data
public class OrgStatisticsQueryDTO {
    private String orgName;
    private Long areaId;
    private Integer status;
}
