package com.yigongbao.module.system.auth.service.impl;

import com.yigongbao.module.system.auth.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 短信服务 Mock 实现（仅非生产环境生效）
 * 通过日志模拟短信发送，后续接入服务商时新增实现类并切换 Profile 即可
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@Profile("!prod")
@Slf4j
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void send(String phone, String content) {
        log.info("【短信模拟】手机号={}，内容={}", phone, content);
    }
}
