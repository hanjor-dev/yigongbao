package com.yigongbao.module.system.auth.service;

import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.ForgotPasswordResetDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.dto.SendCaptchaDTO;
import com.yigongbao.module.system.auth.vo.GraphicCaptchaVO;
import com.yigongbao.module.system.auth.vo.LoginVO;

/**
 * 认证 Service
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface AuthService {

    /**
     * 获取图形验证码（仅 PASSWORD 登录类型使用）
     *
     * @return 验证码 ID 和 Base64 图片
     */
    GraphicCaptchaVO getGraphicCaptcha();

    /**
     * 用户登录（支持 PASSWORD / PHONE / EMAIL 三种方式）
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息（含菜单、权限）
     *
     * @return 当前用户信息
     */
    LoginVO getCurrentUserInfo();

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param dto    修改密码参数
     */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 发送登录验证码（PHONE/EMAIL 登录场景）
     *
     * @param dto 验证码发送参数
     */
    void sendLoginCaptcha(SendCaptchaDTO dto);

    /**
     * 发送忘记密码验证码
     *
     * @param dto 验证码发送参数
     */
    void sendForgotPasswordCaptcha(SendCaptchaDTO dto);

    /**
     * 忘记密码：校验验证码并重置密码
     *
     * @param dto 重置密码参数
     */
    void resetPassword(ForgotPasswordResetDTO dto);
}
