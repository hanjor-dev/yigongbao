package com.yigongbao.module.system.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现（Spring Mail / SMTP）
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringMailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    private final ConfigService configService;

    @Override
    public void send(String to, String subject, String content) {
        // 读取发件人，若未配置则 fail-fast
        String from = configService.getConfigValue(SystemConfigKeyEnum.MAIL_FROM.getKey());
        if (StrUtil.isBlank(from)) {
            log.error("邮件发件人未配置，config_key={}", SystemConfigKeyEnum.MAIL_FROM.getKey());
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        log.info("发送邮件，to={}, subject={}", to, subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        log.info("邮件发送: to={}", to);
    }
}
