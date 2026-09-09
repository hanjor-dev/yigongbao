package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.user.dto.ChangePasswordDTO;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO;
import com.yigongbao.module.system.user.dto.UserPageDTO;
import com.yigongbao.module.system.user.dto.UserStatisticsQueryDTO;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.UserVO;
import com.yigongbao.module.system.user.vo.UserStatisticsVO;
import jakarta.servlet.http.HttpServletResponse;
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
@Tag(name = "用户管理", description = "用户 CRUD、密码管理、状态管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@RequireSign
public class UserController {

    private final UserService userService;

    @Operation(summary = "账户统计")
    @GetMapping("/statistics")
    public Result<UserStatisticsVO> statistics(UserStatisticsQueryDTO query) {
        return Result.success(userService.getStatistics(query));
    }

    /**
     * 分页查询用户列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询用户列表")
    public Result<IPage<UserVO>> list(@Validated @RequestBody UserPageDTO dto) {
        return Result.success(userService.listUser(dto));
    }

    /**
     * 根据ID查询用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户详情")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建用户"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateUserDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新用户"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateUserDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除用户"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        userService.removeUser(id);
        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @Operation(summary = "修改用户状态")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改用户状态"
    )
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
    @Operation(summary = "重置密码")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "重置用户密码",
            logParams = false
    )
    @PutMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改用户密码",
            logParams = false
    )
    @PostMapping("/{id}/change-password")
    public Result<Void> changePassword(
            @PathVariable Long id,
            @Validated @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(id, dto);
        return Result.success();
    }

    /**
     * 用户自更新（仅允许修改手机号和头像）
     */
    @Operation(summary = "用户自更新（仅允许修改手机号和头像）")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新个人资料"
    )
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Validated @RequestBody UpdateUserBySelfDTO dto) {
        // 获取当前登录用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();
        userService.updateUserBySelf(currentUserId, dto);
        return Result.success();
    }

    /**
     * 预览用户名（自动生成模式，预占5分钟）
     *
     * @param orgId 机构ID
     * @return 预占的用户名，手动模式返回 null
     */
    @GetMapping("/username/preview")
    @Operation(summary = "预览用户名（自动生成模式，预占5分钟）")
    public Result<String> previewUsername(@RequestParam Long orgId) {
        return Result.success(userService.previewUsername(orgId));
    }

    /**
     * 导出用户列表为 Excel
     */
    @GetMapping("/export")
    @Operation(summary = "导出用户列表")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.EXPORT,
            operation = "导出用户列表"
    )
    public void export(HttpServletResponse response) {
        userService.exportUsers(response);
    }
}
