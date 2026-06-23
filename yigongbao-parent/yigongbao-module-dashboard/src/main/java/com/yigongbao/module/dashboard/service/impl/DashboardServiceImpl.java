package com.yigongbao.module.dashboard.service.impl;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.RoleCodeEnum;
import com.yigongbao.module.dashboard.service.IDashboardService;
import com.yigongbao.module.dashboard.service.strategy.*;
import com.yigongbao.module.dashboard.vo.DashboardVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据概览服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final UserService userService;
    private final SalesmanDashboardStrategy salesmanStrategy;
    private final DesignerDashboardStrategy designerStrategy;
    private final SuperAdminDashboardStrategy superAdminStrategy;
    private final RegionalManagerDashboardStrategy regionalManagerStrategy;
    private final DesignerManagerDashboardStrategy designerManagerStrategy;
    private final ProductionManagerDashboardStrategy productionManagerStrategy;
    private final ProductionWorkerDashboardStrategy productionWorkerStrategy;
    private final QcDashboardStrategy qcStrategy;
    private final QcManagerDashboardStrategy qcManagerStrategy;
    private final WarehouseManagerDashboardStrategy warehouseManagerStrategy;
    private final FinanceDashboardStrategy financeStrategy;
    private final CompanyAdminDashboardStrategy companyAdminStrategy;

    private final Map<String, DashboardStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        strategyMap.put(RoleCodeEnum.ADMIN.getCode(), superAdminStrategy);
        strategyMap.put(RoleCodeEnum.SALESMAN.getCode(), salesmanStrategy);
        strategyMap.put(RoleCodeEnum.DESIGNER.getCode(), designerStrategy);
        strategyMap.put(RoleCodeEnum.REGIONAL_MANAGER.getCode(), regionalManagerStrategy);
        strategyMap.put(RoleCodeEnum.DESIGNER_MANAGER.getCode(), designerManagerStrategy);
        strategyMap.put(RoleCodeEnum.PRODUCTION_MANAGER.getCode(), productionManagerStrategy);
        strategyMap.put(RoleCodeEnum.PRODUCTION_WORKER.getCode(), productionWorkerStrategy);
        strategyMap.put(RoleCodeEnum.QC.getCode(), qcStrategy);
        strategyMap.put(RoleCodeEnum.QC_MANAGER.getCode(), qcManagerStrategy);
        strategyMap.put(RoleCodeEnum.WAREHOUSE_MANAGER.getCode(), warehouseManagerStrategy);
        strategyMap.put(RoleCodeEnum.FINANCE.getCode(), financeStrategy);
        strategyMap.put(RoleCodeEnum.COMPANY_ADMIN.getCode(), companyAdminStrategy);
    }

    @Override
    public DashboardVO getDashboard(String roleCode, Long userId, DashboardQueryDTO query) {
        log.info("获取数据概览: roleCode={}, userId={}, query={}", roleCode, userId, query);

        // 获取当前用户信息
        UserEntity currentUser = userService.getById(userId);
        if (currentUser == null) {
            log.error("用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // 角色权限验证
        if (!validateRoleAccess(currentUser, roleCode)) {
            log.warn("角色权限校验失败: userId={}, userRole={}, requestRole={}",
                userId, currentUser.getRoleCode(), roleCode);
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, "无权访问该角色数据");
        }

        // 选择策略执行查询
        DashboardStrategy strategy = strategyMap.get(roleCode);
        if (strategy == null) {
            log.error("不支持的角色代码: roleCode={}", roleCode);
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "不支持的角色代码");
        }

        return strategy.buildDashboard(userId, query);
    }

    /**
     * 验证用户是否有权限访问指定角色的数据
     */
    private boolean validateRoleAccess(UserEntity user, String requestRoleCode) {
        String userRoleCode = user.getRoleCode();

        // 超级管理员可以访问所有角色数据
        if (RoleCodeEnum.ADMIN.getCode().equals(userRoleCode)) {
            return true;
        }

        // 其他角色只能访问自己的数据
        return userRoleCode != null && userRoleCode.equals(requestRoleCode);
    }
}
