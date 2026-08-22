package com.yigongbao.flow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.event.SystemConfigChangedEvent;
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

    private volatile Map<Integer, String> cachedColors;

    public String getColor(Integer status) {
        if (status == null) {
            return null;
        }
        return getColors().get(status);
    }

    public Map<Integer, String> getColors() {
        Map<Integer, String> colors = cachedColors;
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

    private Map<Integer, String> loadColors() {
        Map<Integer, String> colors = new HashMap<>(defaultColors());
        String configValue = configService.getConfigValue(SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey());
        if (configValue == null || configValue.isBlank()) {
            return Collections.unmodifiableMap(colors);
        }

        try {
            Map<String, String> configuredColors = objectMapper.readValue(
                    configValue, new TypeReference<Map<String, String>>() { });
            if (configuredColors != null) {
                configuredColors.forEach((statusValue, color) -> addIfValid(colors, statusValue, color));
            }
        } catch (Exception e) {
            log.warn("订单状态颜色配置解析失败，使用默认颜色，configKey={}",
                    SystemConfigKeyEnum.ORDER_STATUS_COLOR.getKey(), e);
        }
        return Collections.unmodifiableMap(colors);
    }

    private void addIfValid(Map<Integer, String> colors, String statusValue, String color) {
        try {
            Integer status = Integer.valueOf(statusValue);
            if (FlowStatusEnum.getByValue(status) == null) {
                log.warn("忽略未知订单状态颜色配置: status={}", statusValue);
                return;
            }
            if (color == null || !HEX_COLOR_PATTERN.matcher(color).matches()) {
                log.warn("忽略非法订单状态颜色配置: status={}, color={}", statusValue, color);
                return;
            }
            colors.put(status, color);
        } catch (NumberFormatException e) {
            log.warn("忽略非法订单状态颜色配置键: status={}", statusValue);
        }
    }

    private Map<Integer, String> defaultColors() {
        Map<Integer, String> colors = new HashMap<>();
        for (FlowStatusEnum status : FlowStatusEnum.values()) {
            colors.put(status.getValue(), defaultColor(status));
        }
        return colors;
    }

    private String defaultColor(FlowStatusEnum status) {
        return switch (status) {
            case DATA_AUDIT_REJECTED, PRINT_FAILED, QC_FAILED -> "#F56C6C";
            case DATA_AUDIT_PASSED, DESIGN_COMPLETED, PRINT_COMPLETED,
                 QC_PASSED, WAREHOUSED, WAREHOUSE_OUT, COMPLETED -> "#67C23A";
            case DRAFT, CANCELLED -> "#909399";
            case PENDING_DATA_AUDIT, PENDING_DESIGN, PENDING_PRINT,
                 REWORK, PACKING -> "#E6A23C";
            default -> "#409EFF";
        };
    }
}
