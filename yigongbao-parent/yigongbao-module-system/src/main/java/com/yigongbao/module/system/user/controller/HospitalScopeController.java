package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 医院权限范围 Controller
 * 处理需要根据用户权限过滤的医院查询，用于业务员创建订单等业务场景
 *
 * @author hanjor
 * @date 2026-03-20
 */
@Tag(name = "用户医院范围管理", description = "管理用户可访问的医院范围")
@RestController
@RequestMapping("/system/hospital-scope")
@RequiredArgsConstructor
public class HospitalScopeController {

    private final UserHospitalService userHospitalService;

    /**
     * 获取指定用户可操作的医院列表（根据用户权限过滤）
     *
     * @param userId 用户ID
     * @return 用户可操作的医院列表
     */
    @Operation(summary = "获取指定用户可操作的医院列表")
    @GetMapping("/my-hospitals/{userId}")
    public Result<List<HospitalVO>> getMyHospitals(@PathVariable Long userId) {
        return Result.success(userHospitalService.getMyHospitalOptions(userId));
    }

    /**
     * 获取当前登录用户可操作的医院列表（从请求上下文获取用户ID）
     *
     * @return 当前用户可操作的医院列表
     */
    @Operation(summary = "获取当前登录用户可操作的医院列表")
    @GetMapping("/my-hospitals")
    public Result<List<HospitalVO>> getMyHospitalsByLogin() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return Result.success(userHospitalService.getMyHospitalOptions(currentUserId));
    }
}
