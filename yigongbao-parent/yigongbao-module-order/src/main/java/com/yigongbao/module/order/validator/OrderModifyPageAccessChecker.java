package com.yigongbao.module.order.validator;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;

import java.util.Set;

/**
 * 订单修改页面访问规则。
 *
 * <p>该规则只判断是否可以进入修改页面，不判断进入页面后是直接修改还是提交申请。</p>
 */
public final class OrderModifyPageAccessChecker {

    private static final Set<String> ADMIN_ROLES = Set.of(
            RoleCodeEnum.ADMIN.getCode(), RoleCodeEnum.COMPANY_ADMIN.getCode());
    private static final Set<String> BUSINESS_ROLES = Set.of(
            RoleCodeEnum.SALESMAN.getCode(),
            RoleCodeEnum.SALESMAN_SELF.getCode(),
            RoleCodeEnum.REGIONAL_MANAGER.getCode());
    private static final Set<String> DESIGNER_ROLES = Set.of(
            RoleCodeEnum.DESIGNER.getCode(),
            RoleCodeEnum.DESIGNER_MANAGER.getCode());

    private OrderModifyPageAccessChecker() {
    }

    /**
     * 判断当前用户是否可以打开订单修改页面。
     *
     * @param order                  订单
     * @param roleCode               当前用户角色
     * @param hasPendingModifyApply  是否存在待审核修改申请
     * @return true=允许打开，false=不允许打开
     */
    public static boolean canModify(OrderMainEntity order, String roleCode,
                                    boolean hasPendingModifyApply) {
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            return false;
        }
        if (hasPendingModifyApply || Integer.valueOf(1).equals(order.getHasPendingCancelApply())) {
            return false;
        }
        if (roleCode == null) {
            return false;
        }

        if (containsRole(ADMIN_ROLES, roleCode)) {
            return true;
        }
        if (containsRole(BUSINESS_ROLES, roleCode)) {
            return isPhase(order, FlowPhaseEnum.ORDER) || isPhase(order, FlowPhaseEnum.DESIGN);
        }
        if (containsRole(DESIGNER_ROLES, roleCode)) {
            return isPhase(order, FlowPhaseEnum.DESIGN);
        }
        return false;
    }

    private static boolean containsRole(Set<String> roles, String roleCode) {
        return roleCode != null && roles.contains(roleCode);
    }

    private static boolean isPhase(OrderMainEntity order, FlowPhaseEnum phase) {
        return phase.getValue().equals(order.getPhase());
    }
}
