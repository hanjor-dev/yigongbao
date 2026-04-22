package com.yigongbao.module.system.auth.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统一登录 DTO
 * loginType=PASSWORD 时 principal=用户名，credential=密码
 * loginType=PHONE    时 principal=手机号，credential=短信验证码
 * loginType=EMAIL    时 principal=邮箱，  credential=邮箱验证码
 * <p>
 * 兼容旧格式：username + password（自动映射为 principal + credential + loginType=PASSWORD）
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class LoginDTO {

    /**
     * 登录类型（PASSWORD/PHONE/EMAIL）
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
     * 图形验证码 ID（仅 PASSWORD 类型必传）
     */
    private String captchaId;

    /**
     * 图形验证码内容（仅 PASSWORD 类型必传）
     */
    private String captchaCode;

    // ==================== 兼容旧格式（username + password）====================

    /**
     * 兼容旧字段 username，自动填充 principal 和 loginType
     */
    @JsonSetter("username")
    public void setUsername(String username) {
        if (this.principal == null) {
            this.principal = username;
        }
        if (this.loginType == null) {
            this.loginType = LoginTypeEnum.PASSWORD;
        }
    }

    /**
     * 兼容旧字段 password，自动填充 credential
     */
    @JsonSetter("password")
    public void setPassword(String password) {
        if (this.credential == null) {
            this.credential = password;
        }
    }
}
