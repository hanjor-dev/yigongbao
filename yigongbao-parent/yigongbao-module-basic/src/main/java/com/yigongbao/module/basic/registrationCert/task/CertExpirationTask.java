package com.yigongbao.module.basic.registrationCert.task;

import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 注册证过期状态刷新任务
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertExpirationTask {

    private final RegistrationCertService registrationCertService;

    /**
     * 每日凌晨 1 点刷新注册证有效状态
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshExpiredStatus() {
        log.info("开始执行注册证过期状态刷新任务");
        registrationCertService.refreshExpiredStatus();
        log.info("注册证过期状态刷新任务执行完成");
    }
}
