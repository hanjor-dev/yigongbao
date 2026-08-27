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
                RoleCodeEnum.ADMIN.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.COMPANY_ADMIN.getCode(), false, false)).isTrue();
    }

    @Test
    void businessGroupCanOpenInOrderOrDesignPhase() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.SALESMAN.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.REGIONAL_MANAGER.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.SALESMAN_SELF.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.PRINT.getValue()),
                RoleCodeEnum.SALESMAN.getCode(), false, false)).isFalse();
    }

    @Test
    void designGroupCanOpenOnlyInDesignPhase() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER_MANAGER.getCode(), false, false)).isTrue();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.DESIGNER.getCode(), false, false)).isFalse();
    }

    @Test
    void pendingModificationApplyBlocksEveryRole() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()),
                RoleCodeEnum.ADMIN.getCode(), true, false)).isFalse();
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.DESIGN.getValue()),
                RoleCodeEnum.DESIGNER_MANAGER.getCode(), true, false)).isFalse();
    }

    @Test
    void pendingCancelApplyBlocksOtherwiseAllowedOrder() {
        OrderMainEntity order = order(FlowPhaseEnum.ORDER.getValue());
        assertThat(OrderModifyPageAccessChecker.canApply(order,
                RoleCodeEnum.SALESMAN.getCode(), false, true)).isFalse();
    }

    @Test
    void missingRoleReturnsFalseWithoutException() {
        assertThat(OrderModifyPageAccessChecker.canApply(order(FlowPhaseEnum.ORDER.getValue()), null, false, false))
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
