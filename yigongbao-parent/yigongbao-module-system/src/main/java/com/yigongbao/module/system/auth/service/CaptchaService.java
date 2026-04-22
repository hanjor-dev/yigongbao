package com.yigongbao.module.system.auth.service;

/**
 * 验证码服务接口
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface CaptchaService {

    /**
     * 发送验证码
     *
     * @param captchaType 验证码类型（PHONE/EMAIL）
     * @param target      目标（手机号或邮箱）
     * @param scene       使用场景（login/forgot）
     */
    void sendCaptcha(String captchaType, String target, String scene);

    /**
     * 校验验证码（匹配后立即删除，防重放攻击；错误5次后删除key强制重发）
     *
     * @param captchaType 验证码类型（PHONE/EMAIL）
     * @param target      目标（手机号或邮箱）
     * @param scene       使用场景（login/forgot）
     * @param code        用户输入的验证码
     */
    void verifyCaptcha(String captchaType, String target, String scene, String code);
}
