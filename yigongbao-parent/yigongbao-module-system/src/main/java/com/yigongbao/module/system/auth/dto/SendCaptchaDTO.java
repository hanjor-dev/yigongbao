package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送验证码 DTO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class SendCaptchaDTO {

    /**
     * 验证码类型（PHONE/EMAIL）
     */
    @NotNull(message = "验证码类型不能为空")
    private CaptchaTypeEnum captchaType;

    /**
     * 目标（手机号或邮箱）
     */
    @NotBlank(message = "目标不能为空")
    private String target;
}
