package com.yigongbao.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemVO {
    private String healthStatus;
    private String avgResponseTime;
    private Integer onlineUsers;
    private String avgOrderCycle;
}
