package com.yigongbao.module.order.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单修改申请清理定时任务
 * 每5分钟执行一次，清理过期申请
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderModifyApplyCleanTask {

    private final OrderModificationApplyMapper applyMapper;

    /**
     * 每5分钟执行一次，清理过期申请
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredApplications() {
        long startTime = System.currentTimeMillis();
        log.info("定时任务开始: taskName=cleanExpiredApplications");

        try {
            LambdaUpdateWrapper<OrderModificationApplyEntity> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.EXPIRED.getCode())
                   .eq(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.PENDING.getCode())
                   .lt(OrderModificationApplyEntity::getExpireTime, LocalDateTime.now());

            int count = applyMapper.update(null, wrapper);
            long duration = System.currentTimeMillis() - startTime;
            log.info("定时任务完成: taskName=cleanExpiredApplications, processedCount={}, duration={}ms", count, duration);

        } catch (Exception e) {
            log.error("定时任务失败: taskName=cleanExpiredApplications", e);
        }
    }
}
