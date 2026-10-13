package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统一登录 DTO
 * loginType=PASSWORD 时 principal=用户名，credential=密码
 * loginType=PHONE    时 principal=手机号，credential=短信验证码
 * <p>
 * PASSWORD 登录必须先通过 /image-captch/check 验证滑动轨迹，获取 captchaToken 后再提交登录
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class LoginDTO {

    /**
     * 登录类型（PASSWORD/PHONE）
     */
    @NotNull(message = "登录类型不能为空")
    private LoginTypeEnum loginType;

    /**
     * 登录凭据主体（用户名/手机号/邮箱）
     */
    @NotBlank(message = "登录账号不能为空")
    private String principal;

    /**
     * 登录凭据（密码/验证码）
     */
    @NotBlank(message = "密码或验证码不能为空")
    private String credential;

    /**
     * 滑动验证码 Token（PASSWORD 类型必填，由 POST /image-captch/check 返回的 id）
     * PHONE 类型无需此字段
     */
    private String captchaToken;
}
