package com.yigongbao.module.system.auth.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * @author hanjor
 * @version 1.0
 * @date 2026/4/22 22:31
 */

@Tag(name = "图像验证码", description = "生成、校验图像验证码")
@RestController
@RequestMapping("/image-captch")
public class ImageCaptchController {

    @Autowired
    private ImageCaptchaApplication application;

    /**
     * 生成验证码
     * @return 验证码数据
     */
    @Operation(summary = "生成图像验证码", description = "参数非必填，默认滑块：SLIDER、ROTATE、WORD_IMAGE_CLICK、CONCAT")
    @GetMapping("/genCaptcha")
    public ApiResponse<ImageCaptchaVO> genCaptcha(@RequestParam(required = false) String type) {
        // 1.生成验证码(该数据返回给前端用于展示验证码数据)
        if (StrUtil.isEmpty(type)) {
            type = CaptchaTypeConstant.SLIDER;
        }
        // 参数1为具体的验证码类型， 默认支持 SLIDER、ROTATE、WORD_IMAGE_CLICK、CONCAT 等验证码类型，详见： `CaptchaTypeConstant`类
        return  application.generateCaptcha(type);
    }

    /**
     * 校验验证码
     * @param data 验证码数据
     * @return 校验结果
     */
    @Operation(summary = "校验验证码")
    @PostMapping("/check")
    @ResponseBody
    public ApiResponse<?> checkCaptcha(@RequestBody Data data) {
        ApiResponse<?> response = application.matching(data.getId(), data.getData());
        if (response.isSuccess()) {
            return ApiResponse.ofSuccess(Collections.singletonMap("id", data.getId()));
        }
        return response;
    }

    @lombok.Data
    public static class Data {
        // 验证码id,前端回传的验证码ID
        private String  id;
        // 验证码数据,前端回传的验证码轨迹数据
        private ImageCaptchaTrack data;
    }
}


