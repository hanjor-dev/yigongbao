package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.ResetPasswordDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.UserVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller
 * 处理用户相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-17
 */
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<IPage<UserVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer accountType,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.listUser(pageNum, pageSize, username, realName, orgId, deptId, accountType, status));
    }

    /**
     * 根据ID查询用户详情
     */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateUserDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateUserDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        userService.removeUser(id);
        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Validated @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/change-password")
    public Result<Void> changePassword(
            @PathVariable Long id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.changePassword(id, oldPassword, newPassword);
        return Result.success();
    }

    /**
     * 用户自更新（仅允许修改手机号和头像）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Validated @RequestBody UpdateUserBySelfDTO dto) {
        // 获取当前登录用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();
        userService.updateUserBySelf(currentUserId, dto);
        return Result.success();
    }
}
