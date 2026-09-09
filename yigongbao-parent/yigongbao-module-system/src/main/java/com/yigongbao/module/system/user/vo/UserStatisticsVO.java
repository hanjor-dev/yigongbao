package com.yigongbao.module.system.user.vo;

import lombok.Data;

/** 账户类型统计结果。 */
@Data
public class UserStatisticsVO {
    private Long total = 0L;
    private Long enterprise = 0L;
    private Long business = 0L;
}
