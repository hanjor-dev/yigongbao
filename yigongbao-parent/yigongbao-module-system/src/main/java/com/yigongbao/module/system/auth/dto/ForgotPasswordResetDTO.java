package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码重置 DTO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class ForgotPasswordResetDTO {

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

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String newPassword;
}
