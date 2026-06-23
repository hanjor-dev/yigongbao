package com.yigongbao.module.dashboard.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.result.Result;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.RoleCodeEnum;
import com.yigongbao.module.dashboard.service.IDashboardService;
import com.yigongbao.module.dashboard.vo.DashboardVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据概览控制器
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @PostMapping("/{roleCode}")
    public Result<DashboardVO> getDashboard(
            @PathVariable String roleCode,
            @RequestBody @Valid DashboardQueryDTO query
    ) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证时间范围参数
        try {
            query.validateCustomRange();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, e.getMessage());
        }

        // 验证角色代码
        try {
            RoleCodeEnum.fromCode(roleCode);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "不支持的角色代码: " + roleCode);
        }

        // 执行查询
        DashboardVO data = dashboardService.getDashboard(roleCode, userId, query);

        return Result.success(data);
    }
}
