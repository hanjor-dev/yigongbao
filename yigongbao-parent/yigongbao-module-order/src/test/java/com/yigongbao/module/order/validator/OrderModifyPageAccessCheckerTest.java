package com.yigongbao.module.order.validator;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderModifyPageAccessCheckerTest {

    @Test
    void adminCanOpenInAnyPhaseWhenNoPendingApply() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.PRINT.getValue()),
                RoleCodeEnum.ADMIN.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.COMPANY_ADMIN.getCode(), false)).isTrue();
    }

    @Test
    void businessGroupCanOpenInOrderOrDesignPhase() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.SALESMAN.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.REGIONAL_MANAGER.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.SALESMAN_SELF.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.PRINT.getValue()),
                RoleCodeEnum.SALESMAN.getCode(), false)).isFalse();
    }

    @Test
    void designGroupCanOpenOnlyInDesignPhase() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER_MANAGER.getCode(), false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.DESIGNER.getCode(), false)).isFalse();
    }

    @Test
    void pendingModificationApplyBlocksEveryRole() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.ADMIN.getCode(), true)).isFalse();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER_MANAGER.getCode(), true)).isFalse();
    }

    @Test
    void pendingCancelApplyBlocksOtherwiseAllowedOrder() {
        OrderMainEntity order = order(FlowPhaseEnum.ORDER.getValue());
        order.setHasPendingCancelApply(1);
        assertThat(OrderModifyPageAccessChecker.canApply(order,
                RoleCodeEnum.SALESMAN.getCode(), false)).isFalse();
    }

    @Test
    void missingRoleReturnsFalseWithoutException() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()), null, false))
                .isFalse();
    }

    private OrderMainEntity order(Integer phase) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setIsDeleted(0);
        order.setPhase(phase);
        return order;
    }
}
