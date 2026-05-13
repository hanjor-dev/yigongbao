package com.yigongbao.module.order.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.enums.OrderDraftStatusEnum;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单草稿清理定时任务
 * 每天凌晨 2:00 执行，清理过期草稿
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDraftCleanupTask {

    private final OrderDraftMapper orderDraftMapper;

    /**
     * 每天凌晨 2:00 执行，清理过期草稿
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredDrafts() {
        log.info("开始清理过期草稿...");
        try {
            LocalDateTime now = LocalDateTime.now();
            // 批量更新：将所有已过期且状态为"有效"的草稿直接更新为"已过期"
            int updated = orderDraftMapper.update(null,
                    new LambdaUpdateWrapper<OrderDraftEntity>()
                            .set(OrderDraftEntity::getStatus, OrderDraftStatusEnum.EXPIRED.getCode())
                            .lt(OrderDraftEntity::getExpiresAt, now)
                            .eq(OrderDraftEntity::getIsDeleted, 0)
                            .eq(OrderDraftEntity::getStatus, OrderDraftStatusEnum.VALID.getCode())
            );
            log.info("过期草稿清理完成，清理数量：{}", updated);
        } catch (Exception e) {
            log.error("清理过期草稿异常", e);
        }
    }
}
