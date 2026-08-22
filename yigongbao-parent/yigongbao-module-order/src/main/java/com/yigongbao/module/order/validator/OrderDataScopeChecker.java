package com.yigongbao.module.order.validator;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 统一校验当前登录用户是否可访问指定订单。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataScopeChecker {

    private final OrderMainMapper orderMainMapper;
    private final OrderQueryHelper orderQueryHelper;
    private final UserHospitalService userHospitalService;

    public void checkOrderAccess(Long orderId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId);
        orderQueryHelper.buildDataScopeCondition(wrapper, currentUserId, scopeType);
        if (orderMainMapper.selectCount(wrapper) == 0) {
            log.warn("订单不在当前用户数据权限范围内，orderId={}, userId={}, scopeType={}",
                    orderId, currentUserId, scopeType);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
    }
}
