package com.yigongbao.module.system.auth.service;

/**
 * 邮件发送服务接口
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface MailService {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void send(String to, String subject, String content);
}
