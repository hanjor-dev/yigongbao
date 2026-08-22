package com.yigongbao.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
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
    void shouldLoadConfiguredColorsAndUseDefaultForMissingStatuses() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn("{\"2020\":\"#123456\"}");

        assertThat(resolver.getColor(2020)).isEqualTo("#123456");
        assertThat(resolver.getColor(1010)).isEqualTo("#909399");
        assertThat(resolver.getColor(null)).isNull();
        assertThat(resolver.getColors()).containsEntry(2020, "#123456");
    }

    @Test
    void shouldCacheParsedColorsUntilCacheIsCleared() {
        when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey()))
                .thenReturn("{\"2020\":\"#123456\"}");

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
                .thenReturn("{\"2020\":\"not-a-color\",\"9999\":\"#ffffff\",\"2030\":\"#abcdef\"}");

        Map<Integer, String> colors = resolver.getColors();

        assertThat(colors).containsEntry(2020, "#409EFF");
        assertThat(colors).doesNotContainKey(9999);
        assertThat(colors).containsEntry(2030, "#abcdef");
    }
}
