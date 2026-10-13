package com.yigongbao.module.order.listener;

import com.yigongbao.common.event.ClassicCaseMarkedEvent;
import com.yigongbao.module.order.service.IClassicCaseFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 经典案例文件迁移监听器（Order模块）
 * <p>
 * 处理order_file表的文件迁移。
 * 同步执行（非@Async），确保文件迁移在标记事务中完成，失败时自动回滚。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClassicCaseFileListener {

    private final IClassicCaseFileService classicCaseFileService;

    @EventListener
    public void handleClassicCaseMarked(ClassicCaseMarkedEvent event) {
        log.info("Order模块开始处理经典案例文件迁移: orderId={}, orderCode={}",
            event.getOrderId(), event.getOrderCode());

        classicCaseFileService.migrateFilesToClassicCase(event.getOrderId(), event.getOrderCode());

        log.info("Order模块文件迁移完成: orderId={}", event.getOrderId());
    }
}
