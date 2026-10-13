package com.yigongbao.module.production.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.device.entity.DeviceUsageCounterEntity;
import com.yigongbao.module.production.device.mapper.DeviceUsageCounterMapper;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 设备每日使用次数计数器服务实现
 * <p>
 * 实现设备每日使用次数的统计和查询功能，使用乐观锁机制保证并发安全。
 * 核心功能包括：
 * 1. 累加计数器（支持并发重试）
 * 2. 查询当日计数（只读操作）
 * </p>
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceUsageCounterServiceImpl extends ServiceImpl<DeviceUsageCounterMapper, DeviceUsageCounterEntity>
        implements IDeviceUsageCounterService {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final DeviceUsageCounterMapper counterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer incrementAndGet(Long deviceId) {
        return incrementAndGet(deviceId, LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer incrementAndGet(Long deviceId, LocalDate usageDate) {
        LocalDate today = usageDate;

        // 乐观锁重试机制
        int maxRetries = MAX_RETRY_ATTEMPTS;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 查询当天记录
                DeviceUsageCounterEntity counter = counterMapper.selectOne(
                    new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                        .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                        .eq(DeviceUsageCounterEntity::getUsageDate, today)
                );

                if (counter == null) {
                    // 首次使用，插入记录
                    counter = new DeviceUsageCounterEntity();
                    counter.setDeviceId(deviceId);
                    counter.setUsageDate(today);
                    counter.setUsageCount(1);
                    counter.setVersion(0);
                    counterMapper.insert(counter);

                    log.info("初始化设备上机次数: deviceId={}, date={}, count=1", deviceId, today);
                    return 1;
                } else {
                    // 累加（乐观锁更新）
                    int updated = counterMapper.update(null,
                        new LambdaUpdateWrapper<DeviceUsageCounterEntity>()
                            .eq(DeviceUsageCounterEntity::getId, counter.getId())
                            .eq(DeviceUsageCounterEntity::getVersion, counter.getVersion())
                            .set(DeviceUsageCounterEntity::getUsageCount, counter.getUsageCount() + 1)
                            .set(DeviceUsageCounterEntity::getVersion, counter.getVersion() + 1)
                    );

                    if (updated > 0) {
                        int newCount = counter.getUsageCount() + 1;
                        log.info("累加设备上机次数: deviceId={}, date={}, count={}", deviceId, today, newCount);
                        return newCount;
                    } else {
                        // 乐观锁冲突，重试
                        log.warn("设备上机次数更新冲突，重试: deviceId={}, retry={}", deviceId, i + 1);
                    }
                }
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，重试
                log.warn("设备上机次数插入冲突，重试: deviceId={}, retry={}", deviceId, i + 1);
            }
        }

        // 重试失败
        throw new BusinessException(ErrorCodeEnum.DEVICE_USAGE_COUNTER_UPDATE_FAILED);
    }

    @Override
    public Integer getTodayCount(Long deviceId) {
        LocalDate today = LocalDate.now();

        DeviceUsageCounterEntity counter = counterMapper.selectOne(
            new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                .eq(DeviceUsageCounterEntity::getUsageDate, today)
        );

        return counter != null ? counter.getUsageCount() : 0;
    }
}
