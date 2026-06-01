package com.yigongbao.module.system.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.auth.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 短信服务生产环境实现（临时Mock实现）
 * <p>
 * 当前生产环境尚未接入真实短信服务商，使用Mock实现：
 * - 若配置了 sms.mock.redirect-email，则将短信内容以邮件方式投递到该邮箱
 * - 否则仅打印日志
 * <p>
 * TODO: 接入真实短信服务商后，替换为真实实现
 *
 * @author hanjor
 * @date 2026-06-01
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdSmsServiceImpl implements SmsService {

    private final MailService mailService;

    @Value("${sms.mock.redirect-email:}")
    private String redirectEmail;

    @Override
    public void send(String phone, String content) {
        log.warn("【生产环境-短信Mock】手机号={}，内容={}", phone, content);

        if (StrUtil.isNotBlank(redirectEmail)) {
            try {
                String subject = "【生产环境-短信Mock】手机号 " + phone;
                mailService.send(redirectEmail, subject, content);
                log.info("【生产环境-短信Mock】已将短信内容转发至邮箱，phone={}, email={}", phone, redirectEmail);
            } catch (Exception e) {
                log.error("【生产环境-短信Mock】邮件转发失败: phone={}, error={}", phone, e.getMessage(), e);
            }
        } else {
            log.warn("【生产环境-短信Mock】未配置redirect-email，短信内容仅记录日志");
        }
    }
}
