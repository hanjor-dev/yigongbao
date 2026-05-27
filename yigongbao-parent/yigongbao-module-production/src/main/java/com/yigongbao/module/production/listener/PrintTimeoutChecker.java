package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印超时检查器
 * 定期检查打印状态超时的流转卡，记录警告日志
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintTimeoutChecker {

    private static final int DEFAULT_PENDING_TIMEOUT = 10;
    private static final int DEFAULT_PRINTING_TIMEOUT = 240;

    private final ProductionRecordMapper recordMapper;
    private final ConfigService configService;

    @Scheduled(fixedDelay = 60000)
    public void checkPrintTimeout() {
        int pendingTimeoutMinutes = getConfigInt(
                SystemConfigKeyEnum.PRODUCTION_PENDING_PRINT_TIMEOUT_MINUTES, DEFAULT_PENDING_TIMEOUT);
        int printingTimeoutMinutes = getConfigInt(
                SystemConfigKeyEnum.PRODUCTION_PRINTING_TIMEOUT_MINUTES, DEFAULT_PRINTING_TIMEOUT);

        LocalDateTime pendingThreshold = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        List<ProductionRecordEntity> pendingTimeout = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .eq(ProductionRecordEntity::getStatus, RecordStatusEnum.PENDING_PRINT.getCode())
                        .isNotNull(ProductionRecordEntity::getPrintDeviceId)
                        .lt(ProductionRecordEntity::getUpdateTime, pendingThreshold));
        pendingTimeout.forEach(record ->
                log.warn("待打印超时提醒: recordId={}, recordNo={}, deviceId={}, 超过{}分钟未收到打印开始推送",
                        record.getId(), record.getRecordNo(), record.getPrintDeviceId(), pendingTimeoutMinutes));

        LocalDateTime printingThreshold = LocalDateTime.now().minusMinutes(printingTimeoutMinutes);
        List<ProductionRecordEntity> printingTimeout = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .eq(ProductionRecordEntity::getStatus, RecordStatusEnum.PRINTING.getCode())
                        .lt(ProductionRecordEntity::getUpdateTime, printingThreshold));
        printingTimeout.forEach(record ->
                log.warn("打印中超时提醒: recordId={}, recordNo={}, deviceId={}, 超过{}分钟未收到打印完成推送",
                        record.getId(), record.getRecordNo(), record.getPrintDeviceId(), printingTimeoutMinutes));
    }

    private int getConfigInt(SystemConfigKeyEnum key, int defaultValue) {
        try {
            String value = configService.getConfigValue(key.getKey());
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("读取配置失败，使用默认值: configKey={}, defaultValue={}", key.getKey(), defaultValue);
            return defaultValue;
        }
    }
}
