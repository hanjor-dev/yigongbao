package com.yigongbao.module.system.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 *
 * @author hanjor
 * @date 2026-03-19
 */
@RestController
@RequestMapping("/api/system/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息（含菜单、权限）
     */
    @GetMapping("/info")
    public Result<LoginVO> getCurrentUserInfo() {
        return Result.success(authService.getCurrentUserInfo());
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        authService.changePassword(userId, dto);
        return Result.success();
    }
}
