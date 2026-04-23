package com.yigongbao.module.system.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.auth.service.SmsService;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 * 支持手机短信/邮箱验证码的发送与校验，含冷却、每日上限、错误次数保护
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final MailService mailService;
    private final ConfigService configService;

    /** 最大错误次数，超过后强制删除 key */
    private static final int MAX_ATTEMPTS = 5;

    // ==================== 公共方法 ====================

    @Override
    public void sendCaptcha(String captchaType, String target, String scene) {
        log.info("发送验证码，type={}, target={}, scene={}", captchaType, target, scene);

        // 1. 冷却检测
        String cooldownKey = buildCooldownKey(scene, captchaType, target);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            log.warn("验证码发送过于频繁，type={}, target={}", captchaType, target);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TOO_FREQUENT);
        }

        // 2. 每日次数检测
        String dailyKey = buildDailyKey(scene, captchaType, target);
        Object dailyCountObj = redisTemplate.opsForValue().get(dailyKey);
        int dailyLimit = getDailyLimit();
        if (dailyCountObj != null && toInt(dailyCountObj) >= dailyLimit) {
            log.warn("验证码每日发送次数已达上限，type={}, target={}", captchaType, target);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_DAILY_LIMIT);
        }

        // 3. 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 写入 Redis
        int expireSeconds = getExpireSeconds();
        int cooldownSeconds = getCooldownSeconds();
        String captchaKey = buildCaptchaKey(scene, captchaType, target);

        redisTemplate.opsForValue().set(captchaKey, code, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);

        Long newCount = redisTemplate.opsForValue().increment(dailyKey);
        if (newCount != null && newCount == 1) {
            // 首次写入，设置到明日0点过期（简化为24小时）
            redisTemplate.expire(dailyKey, 24, TimeUnit.HOURS);
        }

        // 5. 分发发送
        dispatch(captchaType, target, code);
        log.info("验证码发送成功，type={}, target={}", captchaType, target);
    }

    @Override
    public void verifyCaptcha(String captchaType, String target, String scene, String code) {
        log.info("校验验证码，type={}, target={}, scene={}", captchaType, target, scene);

        String captchaKey = buildCaptchaKey(scene, captchaType, target);
        String attemptsKey = buildAttemptsKey(scene, captchaType, target);

        // 1. 验证码是否存在
        String storedCode = (String) redisTemplate.opsForValue().get(captchaKey);
        if (storedCode == null) {
            log.warn("验证码不存在或已过期，type={}, target={}", captchaType, target);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_EXPIRED);
        }

        // 2. 错误次数是否已达上限
        Object attemptsObj = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsObj == null ? 0 : toInt(attemptsObj);
        if (attempts >= MAX_ATTEMPTS) {
            log.warn("验证码错误次数已达上限，强制删除，type={}, target={}", captchaType, target);
            redisTemplate.delete(captchaKey);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_EXPIRED);
        }

        // 3. 校验验证码
        if (!storedCode.equals(code)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            log.warn("验证码不匹配，type={}, target={}, attempts={}", captchaType, target, attempts + 1);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        // 4. 校验成功，删除验证码和错误次数
        redisTemplate.delete(captchaKey);
        redisTemplate.delete(attemptsKey);
        log.info("验证码校验成功，type={}, target={}", captchaType, target);
    }

    // ==================== 私有方法 ====================

    /**
     * 分发验证码发送（短信或邮件）
     */
    private void dispatch(String captchaType, String target, String code) {
        String message = "您的验证码为：" + code + "，请在有效期内使用。";
        if (CaptchaTypeEnum.PHONE.getValue().equals(captchaType)) {
            smsService.send(target, message);
        } else if (CaptchaTypeEnum.EMAIL.getValue().equals(captchaType)) {
            mailService.send(target, "医工宝验证码", message);
        } else {
            log.error("不支持的验证码类型，type={}", captchaType);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TYPE_INVALID);
        }
    }

    private String buildCaptchaKey(String scene, String type, String target) {
        return "captcha:" + scene + ":" + type + ":" + target;
    }

    private String buildCooldownKey(String scene, String type, String target) {
        return "captcha:cooldown:" + scene + ":" + type + ":" + target;
    }

    private String buildDailyKey(String scene, String type, String target) {
        String date = LocalDate.now().toString().replace("-", "");
        return "captcha:daily:" + scene + ":" + type + ":" + target + ":" + date;
    }

    private String buildAttemptsKey(String scene, String type, String target) {
        return "captcha:attempts:" + scene + ":" + type + ":" + target;
    }

    private int getExpireSeconds() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_EXPIRE_SECONDS.getKey());
        if (StrUtil.isNotBlank(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("captcha.expire.seconds 配置值无效，val={}", val);
            }
        }
        return 300;
    }

    private int getCooldownSeconds() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_COOLDOWN_SECONDS.getKey());
        if (StrUtil.isNotBlank(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("captcha.cooldown.seconds 配置值无效，val={}", val);
            }
        }
        return 60;
    }

    private int getDailyLimit() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_DAILY_LIMIT.getKey());
        if (StrUtil.isNotBlank(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("captcha.daily.limit 配置值无效，val={}", val);
            }
        }
        return 10;
    }

    /**
     * Redis 中通过 INCR 写入的计数值，反序列化后可能为 Integer/Long/String，统一转为 int
     */
    private int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(obj.toString());
    }
}
