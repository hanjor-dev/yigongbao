package com.yigongbao.module.order.convert;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderConvertTest {

    @Test
    void trialOrderWithoutRegionalAuditUsesDesignAuditStage() {
        OrderConvert convert = new OrderConvert(mock(UserService.class));
        OrderMainEntity order = new OrderMainEntity();
        order.setBusinessType("11.3");
        order.setDesignAuditStatus(0);
        OrderDetailVO vo = new OrderDetailVO();

        convert.fillAuditInfo(order, vo);

        assertThat(vo.getAuditProgress()).isEqualTo("等待设计管理员审核");
        assertThat(vo.getAuditStage()).isEqualTo("DESIGN_PENDING");
    }
}
