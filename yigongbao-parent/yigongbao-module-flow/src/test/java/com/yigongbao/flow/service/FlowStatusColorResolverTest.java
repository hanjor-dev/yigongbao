package com.yigongbao.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.vo.StatusColorVO;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowStatusColorResolverTest {

    @Mock
    private ConfigService configService;

    private FlowStatusColorResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FlowStatusColorResolver(configService, new ObjectMapper());
    }

    @Test
    void shouldLoadConfiguredColorsAsTagColorObjects() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn("{\"2020\":{\"bgColor\":\"#eff6ff\",\"bdColor\":\"#bfdbfe\",\"color\":\"#2563eb\"}}");

        assertThat(resolver.getColor(2020))
                .isEqualTo(new StatusColorVO("#eff6ff", "#bfdbfe", "#2563eb"));
        assertThat(resolver.getColor(1010)).isNull();
        assertThat(resolver.getColor(null)).isNull();
        assertThat(resolver.getColors()).containsEntry(2020,
                new StatusColorVO("#eff6ff", "#bfdbfe", "#2563eb"));
    }

    @Test
    void shouldCacheParsedColorsUntilCacheIsCleared() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn("{\"2020\":{\"bgColor\":\"#eff6ff\",\"bdColor\":\"#bfdbfe\",\"color\":\"#2563eb\"}}");

        resolver.getColor(2020);
        resolver.getColor(2020);
        verify(configService, times(1))
                .getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey());

        resolver.clearCache();
        resolver.getColor(2020);
        verify(configService, times(2))
                .getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey());
    }

    @Test
    void shouldIgnoreInvalidConfiguredColorsAndUnknownStatuses() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn("{\"2020\":{\"bgColor\":\"not-a-color\",\"bdColor\":\"#bfdbfe\",\"color\":\"#2563eb\"},"
                        + "\"9999\":{\"bgColor\":\"#eff6ff\",\"bdColor\":\"#bfdbfe\",\"color\":\"#2563eb\"},"
                        + "\"2030\":{\"bgColor\":\"#f0fdf4\",\"bdColor\":\"#bbf7d0\",\"color\":\"#16a34a\"}}");

        Map<Integer, StatusColorVO> colors = resolver.getColors();

        assertThat(colors).doesNotContainKey(2020);
        assertThat(colors).doesNotContainKey(9999);
        assertThat(colors).containsEntry(2030,
                new StatusColorVO("#f0fdf4", "#bbf7d0", "#16a34a"));
    }

    @Test
    void shouldReturnNullWhenConfigurationCannotBeRead() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn(null);

        assertThat(resolver.getColor(2020)).isNull();
        assertThat(resolver.getColors()).isEmpty();
    }
}
