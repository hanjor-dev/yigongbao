package com.yigongbao.module.order.utils;

import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderModifyTimeWindowCheckerTest {

    @Mock
    private ConfigService configService;
    @InjectMocks
    private OrderModifyTimeWindowChecker checker;

    @Test
    void nullCreateTime_isOutsideWindowAndElapsedIsMinusOne() {
        assertThat(checker.isWithinTimeWindow(null)).isFalse();
        assertThat(checker.getElapsedMinutes(null)).isEqualTo(-1);
    }

    @Test
    void recentCreateTime_isInsideConfiguredWindow() {
        when(configService.getConfigValueAsInt(
                eq(SystemConfigKeyEnum.ORDER_MODIFY_WINDOW_MINUTES.getKey()), eq(10)))
                .thenReturn(10);

        assertThat(checker.isWithinTimeWindow(LocalDateTime.now().minusMinutes(5))).isTrue();
    }

    @Test
    void oldOrFutureCreateTime_isOutsideWindow() {
        when(configService.getConfigValueAsInt(
                eq(SystemConfigKeyEnum.ORDER_MODIFY_WINDOW_MINUTES.getKey()), eq(10)))
                .thenReturn(10);

        assertThat(checker.isWithinTimeWindow(LocalDateTime.now().minusMinutes(11))).isFalse();
        assertThat(checker.isWithinTimeWindow(LocalDateTime.now().plusMinutes(1))).isFalse();
    }
}
