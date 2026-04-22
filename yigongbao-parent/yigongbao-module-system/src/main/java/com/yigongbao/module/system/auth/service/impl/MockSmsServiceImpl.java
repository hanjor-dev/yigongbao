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
 * 短信服务 Mock 实现（仅非生产环境生效）
 * <p>
 * 开发阶段真实短信服务商尚未购买，以邮件模拟短信发送：
 * - 若配置了 sms.mock.redirect-email，则将短信内容以邮件方式投递到该邮箱，方便在邮箱中查看验证码
 * - 否则仅打印日志
 * <p>
 * 接入真实短信服务商时：新增 ProdSmsServiceImpl（@Profile("prod")），本类无需修改
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class MockSmsServiceImpl implements SmsService {

    private final MailService mailService;

    /**
     * 短信 Mock 重定向邮箱，配置后将短信内容转发到此邮箱
     * 在 application-dev.yml 中通过 sms.mock.redirect-email 配置
     */
    @Value("${sms.mock.redirect-email:}")
    private String redirectEmail;

    @Override
    public void send(String phone, String content) {
        log.info("【短信模拟】手机号={}，内容={}", phone, content);
        // 若配置了重定向邮箱，则将短信内容以邮件方式投递，方便开发阶段接收验证码
        if (StrUtil.isNotBlank(redirectEmail)) {
            try {
                String subject = "【短信模拟】手机号 " + phone;
                mailService.send(redirectEmail, subject, content);
                log.info("【短信模拟】已将短信内容转发至邮箱，phone={}, email={}", phone, redirectEmail);
            } catch (Exception e) {
                log.warn("【短信模拟】邮件转发失败，降级为日志输出，phone={}, error={}", phone, e.getMessage());
            }
        }
    }
}
