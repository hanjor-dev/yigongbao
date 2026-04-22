package com.yigongbao.module.system.auth.service;

/**
 * 短信发送服务接口
 * 当前由 MockSmsServiceImpl 提供 Mock 实现（打日志）
 * 生产环境接入短信服务商后，新增实现类并移除 @Profile("!prod") 限制即可
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface SmsService {

    /**
     * 发送短信
     *
     * @param phone   手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
