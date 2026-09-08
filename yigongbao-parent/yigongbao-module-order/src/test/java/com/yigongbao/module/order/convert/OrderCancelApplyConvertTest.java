package com.yigongbao.module.order.convert;

import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCancelApplyConvertTest {

    @Test
    void toVO_copiesOriginalAndPublicOrderCodes() {
        OrderCancelApplyConvert convert = new OrderCancelApplyConvert();
        OrderCancelApplyEntity entity = new OrderCancelApplyEntity();
        CancelApplyVO result = convert.toVO(entity, "申请人", "审核人", "ORD-001", "YGABC123456");

        assertEquals("ORD-001", result.getOrderCode());
        assertEquals("YGABC123456", result.getPublicOrderCode());
    }
}
