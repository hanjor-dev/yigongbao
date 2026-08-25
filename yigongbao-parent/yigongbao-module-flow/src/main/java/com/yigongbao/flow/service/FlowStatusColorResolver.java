package com.yigongbao.flow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.event.SystemConfigChangedEvent;
import com.yigongbao.common.vo.StatusColorVO;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 订单和流转卡状态颜色解析器。
 * 配置只在首次使用时从 sys_config 读取并解析，列表逐行查询只访问内存 Map。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowStatusColorResolver {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    private volatile Map<Integer, StatusColorVO> cachedColors;

    public StatusColorVO getColor(Integer status) {
        if (status == null) {
            return null;
        }
        return getColors().get(status);
    }

    public Map<Integer, StatusColorVO> getColors() {
        Map<Integer, StatusColorVO> colors = cachedColors;
        if (colors == null) {
            synchronized (this) {
                colors = cachedColors;
                if (colors == null) {
                    colors = loadColors();
                    cachedColors = colors;
                }
            }
        }
        return colors;
    }

    public synchronized void clearCache() {
        cachedColors = null;
    }

    @TransactionalEventListener
    public void onConfigChanged(SystemConfigChangedEvent event) {
        if (SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey().equals(event.getConfigKey())) {
            clearCache();
        }
    }

    private Map<Integer, StatusColorVO> loadColors() {
        Map<Integer, StatusColorVO> colors = new HashMap<>();
        String configValue = configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey());
        if (configValue == null || configValue.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, StatusColorVO> configuredColors = objectMapper.readValue(
                    configValue, new TypeReference<Map<String, StatusColorVO>>() { });
            if (configuredColors != null) {
                configuredColors.forEach((statusValue, color) -> addIfValid(colors, statusValue, color));
            }
        } catch (Exception e) {
            log.warn("订单状态颜色配置解析失败，返回空颜色映射，configKey={}",
                    SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey(), e);
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(colors);
    }

    private void addIfValid(Map<Integer, StatusColorVO> colors, String statusValue, StatusColorVO color) {
        try {
            Integer status = Integer.valueOf(statusValue);
            if (FlowStatusEnum.getByValue(status) == null) {
                log.warn("忽略未知订单状态颜色配置: status={}", statusValue);
                return;
            }
            if (!isValidColor(color)) {
                log.warn("忽略非法订单状态颜色配置: status={}, color={}", statusValue, color);
                return;
            }
            colors.put(status, color);
        } catch (NumberFormatException e) {
            log.warn("忽略非法订单状态颜色配置键: status={}", statusValue);
        }
    }

    private boolean isValidColor(StatusColorVO color) {
        return color != null
                && isValidHex(color.getBgColor())
                && isValidHex(color.getBdColor())
                && isValidHex(color.getColor());
    }

    private boolean isValidHex(String color) {
        return color != null && HEX_COLOR_PATTERN.matcher(color).matches();
    }
}
