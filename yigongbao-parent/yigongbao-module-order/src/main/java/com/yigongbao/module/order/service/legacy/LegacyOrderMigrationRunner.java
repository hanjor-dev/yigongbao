package com.yigongbao.module.order.service.legacy;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyOrderMigrationRunner {
    private final ObjectProvider<LegacyOrderMigrationService> serviceProvider;

    @Async("legacyMigrationTaskExecutor")
    public void runAsync(Long taskId) {
        serviceProvider.getObject().run(taskId);
    }
}
