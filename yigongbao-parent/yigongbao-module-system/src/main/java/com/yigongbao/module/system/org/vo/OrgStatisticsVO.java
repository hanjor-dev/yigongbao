package com.yigongbao.module.system.org.vo;

import lombok.Data;

/** 机构类型统计结果。 */
@Data
public class OrgStatisticsVO {
    private Long total = 0L;
    private Long distributor = 0L;
    private Long serviceProvider = 0L;
    private Long medicalInstitution = 0L;
}
