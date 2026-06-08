package com.yigongbao.module.system.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.ForgotPasswordResetDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.dto.SendCaptchaDTO;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.vo.LoginLogVO;
import com.yigongbao.module.system.auth.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证 Controller
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Tag(name = "认证管理", description = "用户登录、登出、获取当前用户信息、修改密码")
@RestController
@RequestMapping("/system/auth")
@RequiredArgsConstructor
@RequireSign
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.LOGIN,
            operation = "用户登录"
    )
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.LOGOUT,
            operation = "用户登出"
    )
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息（含菜单、权限）
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息（含菜单、权限）")
    public Result<LoginVO> getCurrentUserInfo() {
        return Result.success(authService.getCurrentUserInfo());
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改密码",
            logParams = false
    )
    @PutMapping("/password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        authService.changePassword(userId, dto);
        return Result.success();
    }

    /**
     * 发送登录验证码（PHONE/EMAIL 登录时使用）
     */
    @Operation(summary = "发送登录验证码")
    @PostMapping("/captcha")
    public Result<Void> sendLoginCaptcha(@Validated @RequestBody SendCaptchaDTO dto) {
        authService.sendLoginCaptcha(dto);
        return Result.success();
    }

    /**
     * 发送忘记密码验证码
     */
    @Operation(summary = "发送忘记密码验证码")
    @PostMapping("/forgot-password/captcha")
    public Result<Void> sendForgotPasswordCaptcha(@Validated @RequestBody SendCaptchaDTO dto) {
        authService.sendForgotPasswordCaptcha(dto);
        return Result.success();
    }

    /**
     * 忘记密码：校验验证码并重置密码
     */
    @Operation(summary = "忘记密码重置")
    @PostMapping("/forgot-password/reset")
    public Result<Void> resetPassword(@Validated @RequestBody ForgotPasswordResetDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }

    /**
     * 获取上一次登录记录（用于被踢出时查看）
     */
    @Operation(summary = "获取上一次登录记录")
    @GetMapping("/previous-login")
    public Result<LoginLogVO> getPreviousLogin() {
        return Result.success(authService.getPreviousLogin());
    }

    /**
     * 获取登录历史记录
     */
    @Operation(summary = "获取登录历史")
    @GetMapping("/login-history")
    public Result<List<LoginLogVO>> getLoginHistory(
            @RequestParam(defaultValue = "30") Integer limit) {
        return Result.success(authService.getLoginHistory(limit));
    }
}
