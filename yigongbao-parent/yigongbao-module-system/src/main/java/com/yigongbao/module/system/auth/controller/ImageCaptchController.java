package com.yigongbao.module.system.auth.controller;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.auth.service.ImageCaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图像行为验证码 Controller
 * 提供滑动验证码生成与校验，校验成功后颁发 token 供登录接口二次验证
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Tag(name = "图像行为验证码", description = "生成、校验行为验证码")
@RestController
@RequestMapping("/image-captch")
@RequiredArgsConstructor
public class ImageCaptchController {

    private final ImageCaptchaService imageCaptchaService;

    /**
     * 生成验证码
     *
     * @param type 验证码类型，默认滑块（SLIDER）
     * @return 验证码数据
     */
    @Operation(summary = "生成图像验证码", description = "参数非必填，默认滑块：SLIDER、ROTATE、WORD_IMAGE_CLICK、CONCAT")
    @PostMapping("/genCaptcha")
    public Result<ImageCaptchaVO> genCaptcha(@RequestParam(required = false) String type) {
        ImageCaptchaVO captchaVO = imageCaptchaService.generateCaptcha(type);
        return Result.success(captchaVO);
    }

    /**
     * 校验验证码并颁发二次验证 Token
     * <p>
     * 校验成功后，生成 UUID token 存入 Redis（默认 2 分钟有效期），
     * 前端需将返回的 token 传入登录接口完成二次验证。
     * token 验证成功后立即失效，不可重复使用。
     *
     * @param data 滑动轨迹数据
     * @return 校验成功后返回 token
     */
    @Operation(summary = "校验验证码并颁发二次验证 Token")
    @PostMapping("/check")
    public Result<Map<String, String>> checkCaptcha(@RequestBody Data data) {
        String token = imageCaptchaService.verifyAndGenerateToken(data.getId(), data.getData());
        return Result.success(Map.of("id", token));
    }

    @lombok.Data
    public static class Data {
        // 验证码id,前端回传的验证码ID
        private String  id;
        // 验证码数据,前端回传的验证码轨迹数据
        private ImageCaptchaTrack data;
    }
}
