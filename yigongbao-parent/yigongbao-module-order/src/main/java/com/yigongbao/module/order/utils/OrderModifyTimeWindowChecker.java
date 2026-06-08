package com.yigongbao.module.order.utils;

import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 订单修改时间窗口检查工具类
 * 用于判断订单是否在允许直接修改的时间窗口内
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Component
@RequiredArgsConstructor
public class OrderModifyTimeWindowChecker {

    private final ConfigService configService;

    /**
     * 判断订单是否在修改时间窗口内
     *
     * @param orderCreateTime 订单创建时间
     * @return true=在窗口内可直接修改，false=需要申请
     */
    public boolean isWithinTimeWindow(LocalDateTime orderCreateTime) {
        if (orderCreateTime == null) {
            return false;
        }
        Integer timeWindow = configService.getConfigValueAsInt(
                SystemConfigKeyEnum.ORDER_MODIFY_WINDOW_MINUTES.getKey(), 10
        );
        long elapsedMinutes = ChronoUnit.MINUTES.between(orderCreateTime, LocalDateTime.now());
        return elapsedMinutes >= 0 && elapsedMinutes <= timeWindow;
    }

    /**
     * 获取已过时间（分钟）
     *
     * @param orderCreateTime 订单创建时间
     * @return 已过时间（分钟），若orderCreateTime为null则返回-1
     */
    public long getElapsedMinutes(LocalDateTime orderCreateTime) {
        if (orderCreateTime == null) {
            return -1;
        }
        return ChronoUnit.MINUTES.between(orderCreateTime, LocalDateTime.now());
    }
}
