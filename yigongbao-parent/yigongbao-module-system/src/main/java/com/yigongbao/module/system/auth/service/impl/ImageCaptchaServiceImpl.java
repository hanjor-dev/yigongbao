package com.yigongbao.module.system.auth.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.service.ImageCaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 图像行为验证码 Service 实现
 * 封装滑动验证码的生成、校验及二次验证 token 管理
 *
 * @author hanjor
 * @date 2026-04-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCaptchaServiceImpl implements ImageCaptchaService {

    /**
     * 二次验证 Token 有效期（分钟）
     * 滑动验证成功后颁发的 token 有效期
     */
    private static final int TOKEN_TTL_MINUTES = 2;

    private final ImageCaptchaApplication imageCaptchaApplication;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public ImageCaptchaVO generateCaptcha(String type) {
        log.info("生成图像行为验证码，type={}", type);
        // type 为空时使用默认类型（滑块）
        if (StrUtil.isEmpty(type)) {
            type = CaptchaTypeConstant.SLIDER;
        }
        ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha(type);
        if (!response.isSuccess()) {
            log.error("生成图像行为验证码失败，type={}, code={}", type, response.getCode());
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
        return response.getData();
    }

    @Override
    public String verifyAndGenerateToken(String captchaId, ImageCaptchaTrack trackData) {
        log.info("校验滑动轨迹并颁发二次验证 Token，captchaId={}", captchaId);
        // 1. 校验滑动轨迹
        ApiResponse<?> response = imageCaptchaApplication.matching(captchaId, trackData);
        if (!response.isSuccess()) {
            log.warn("滑动轨迹校验失败，captchaId={}", captchaId);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR);
        }

        // 2. 校验成功，生成 token 并存入 Redis
        String token = IdUtil.fastSimpleUUID();
        String redisKey = ImageCaptchaService.CAPTCHA_SECONDARY_TOKEN_PREFIX + token;
        stringRedisTemplate.opsForValue().set(redisKey, captchaId, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        log.info("二次验证 Token 生成成功，token={}, ttl={}min", token, TOKEN_TTL_MINUTES);
        return token;
    }
}
