package com.yigongbao.module.system.auth.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.enums.CaptchaSceneEnum;
import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.auth.service.SmsService;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CaptchaService 单元测试
 *
 * @author hanjor
 * @date 2026-04-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CaptchaService 单元测试")
class CaptchaServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SmsService smsService;
    @Mock
    private MailService mailService;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    private static final String PHONE = "13800138000";
    private static final String EMAIL = "test@example.com";
    private static final String SCENE = CaptchaSceneEnum.LOGIN.getScene();
    private static final String TYPE_PHONE = CaptchaTypeEnum.PHONE.getValue();
    private static final String TYPE_EMAIL = CaptchaTypeEnum.EMAIL.getValue();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_EXPIRE_SECONDS.getKey())).thenReturn("300");
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_COOLDOWN_SECONDS.getKey())).thenReturn("60");
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_DAILY_LIMIT.getKey())).thenReturn("10");
    }

    // ==================== sendCaptcha 测试 ====================

    @Test
    @DisplayName("sendCaptcha: 冷却中时抛出 CAPTCHA_TOO_FREQUENT")
    void sendCaptcha_whenCoolingDown_shouldThrowTooFrequent() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_TOO_FREQUENT.getCode(), ex.getCode());
        verify(smsService, never()).send(any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: 每日次数已达上限时抛出 CAPTCHA_DAILY_LIMIT")
    void sendCaptcha_whenDailyLimitReached_shouldThrowDailyLimit() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String dailyKey = "captcha:daily:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE + ":" + java.time.LocalDate.now().toString().replace("-", "");
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(dailyKey)).thenReturn("10");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_DAILY_LIMIT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendCaptcha: PHONE 正常发送时调用 SmsService")
    void sendCaptcha_phone_shouldCallSmsService() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE);

        verify(smsService, times(1)).send(eq(PHONE), anyString());
        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: EMAIL 正常发送时调用 MailService")
    void sendCaptcha_email_shouldCallMailService() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_EMAIL + ":" + EMAIL;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        captchaService.sendCaptcha(TYPE_EMAIL, EMAIL, SCENE);

        verify(mailService, times(1)).send(eq(EMAIL), anyString(), anyString());
        verify(smsService, never()).send(any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: 不支持的类型时抛出 CAPTCHA_TYPE_INVALID")
    void sendCaptcha_invalidType_shouldThrowInvalid() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":UNKNOWN:" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha("UNKNOWN", PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_TYPE_INVALID.getCode(), ex.getCode());
    }

    // ==================== verifyCaptcha 测试 ====================

    @Test
    @DisplayName("verifyCaptcha: 验证码不存在时抛出 CAPTCHA_EXPIRED")
    void verifyCaptcha_whenNotExists_shouldThrowExpired() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "123456"));
        assertEquals(ErrorCodeEnum.CAPTCHA_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("verifyCaptcha: 错误次数达到5次时删除 key 并抛出 CAPTCHA_EXPIRED")
    void verifyCaptcha_whenAttemptsExceeded_shouldDeleteAndThrowExpired() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn("5");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "999999"));
        assertEquals(ErrorCodeEnum.CAPTCHA_EXPIRED.getCode(), ex.getCode());
        verify(redisTemplate, times(1)).delete(captchaKey);
    }

    @Test
    @DisplayName("verifyCaptcha: 验证码不匹配时递增错误次数并抛出 CAPTCHA_ERROR")
    void verifyCaptcha_whenMismatch_shouldIncrementAttemptsAndThrowError() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn("2");
        when(valueOps.increment(attemptsKey)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "999999"));
        assertEquals(ErrorCodeEnum.CAPTCHA_ERROR.getCode(), ex.getCode());
        verify(valueOps, times(1)).increment(attemptsKey);
    }

    @Test
    @DisplayName("verifyCaptcha: 验证码匹配时删除 captcha key 和 attempts key")
    void verifyCaptcha_whenMatch_shouldDeleteBothKeys() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn(null);

        assertDoesNotThrow(() -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "123456"));
        verify(redisTemplate, times(1)).delete(captchaKey);
        verify(redisTemplate, times(1)).delete(attemptsKey);
    }
}
