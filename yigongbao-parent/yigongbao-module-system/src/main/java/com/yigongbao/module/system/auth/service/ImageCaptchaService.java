package com.yigongbao.module.system.auth.service;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;

/**
 * 图像行为验证码 Service
 * 封装滑动验证码的生成、校验及二次验证 token 管理
 *
 * @author hanjor
 * @date 2026-04-23
 */
public interface ImageCaptchaService {

    /** Redis key 前缀 */
    String CAPTCHA_SECONDARY_TOKEN_PREFIX = "captcha:secondary:";

    /**
     * 生成图像行为验证码
     *
     * @param type 验证码类型，默认滑块（SLIDER）
     * @return 验证码数据
     */
    ImageCaptchaVO generateCaptcha(String type);

    /**
     * 校验滑动轨迹并颁发二次验证 Token
     * <p>
     * 校验成功后，生成 UUID token 存入 Redis（默认 2 分钟有效期），
     * token 验证成功后立即失效，不可重复使用。
     *
     * @param captchaId 验证码 id（由 generateCaptcha 返回）
     * @param trackData 滑动轨迹数据
     * @return 二次验证 token（供登录接口二次验证使用）
     */
    String verifyAndGenerateToken(String captchaId, ImageCaptchaTrack trackData);
}
