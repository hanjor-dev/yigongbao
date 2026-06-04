package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderMainServiceImpl 审核功能单元测试
 *
 * @author Kiro AI
 * @date 2026-06-04
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderMainServiceImplAuditTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private UserService userService;

    @Mock
    private FlowFacade flowFacade;

    @InjectMocks
    private OrderMainServiceImpl orderMainService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orderMainService, orderMainMapper);
    }

    /**
     * 构建测试订单
     */
    private OrderMainEntity buildOrder(Long id, String businessType, Integer regionalStatus, Integer designStatus) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setBusinessType(businessType);
        order.setRegionalAuditStatus(regionalStatus);
        order.setDesignAuditStatus(designStatus);
        order.setVersion(0);
        order.setStatus(1020); // 待审核状态
        return order;
    }

    /**
     * 构建测试用户
     */
    private UserEntity buildUser(Long id, String roleCode) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRoleCode(roleCode);
        return user;
    }

    @Test
    void testTrialOrder_RegionalManagerAuditPass() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            OrderMainEntity order = buildOrder(orderId, "11.3", 0, 0);
            UserEntity user = buildUser(userId, "regional-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);
            when(orderMainMapper.update(any(), any())).thenReturn(1);

            // When
            orderMainService.auditPass(orderId, userId);

            // Then
            verify(orderMainMapper, times(1)).update(any(), any());
            verify(flowFacade, never()).executeFlow(any());
        }
    }

    @Test
    void testTrialOrder_DesignManagerAuditPass() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            OrderMainEntity order = buildOrder(orderId, "11.3", 1, 0); // 区域已通过
            UserEntity user = buildUser(userId, "designer-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);
            when(orderMainMapper.update(any(), any())).thenReturn(1);

            // When
            orderMainService.auditPass(orderId, userId);

            // Then
            verify(orderMainMapper, times(1)).update(any(), any());
            verify(flowFacade, times(1)).executeFlow(any());
        }
    }

    @Test
    void testTrialOrder_DesignManagerReject() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            String remark = "数据不符合要求";
            OrderMainEntity order = buildOrder(orderId, "11.3", 1, 0);
            UserEntity user = buildUser(userId, "designer-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);
            when(orderMainMapper.update(any(), any())).thenReturn(1);

            // When
            orderMainService.auditReject(orderId, remark, userId);

            // Then
            verify(orderMainMapper, times(1)).update(any(), any());
            verify(flowFacade, times(1)).executeFlow(any());
        }
    }

    @Test
    void testTrialOrder_Resubmit_AfterDesignReject() {
        // Given
        Long orderId = 1L;
        OrderMainEntity order = buildOrder(orderId, "11.3", 1, 2); // 区域通过，设计驳回
        order.setStatus(1040); // 审核不通过状态

        when(orderMainMapper.selectById(orderId)).thenReturn(order);
        when(orderMainMapper.updateById(any())).thenReturn(1);

        // When
        orderMainService.resubmit(orderId);

        // Then
        verify(orderMainMapper, times(1)).updateById(any());
        verify(flowFacade, times(1)).executeFlow(any());

        // 验证Solution A：所有审核状态都被重置
        assertEquals(0, order.getRegionalAuditStatus());
        assertEquals(0, order.getDesignAuditStatus());
    }

    @Test
    void testBusinessOrder_DesignManagerAuditPass() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            OrderMainEntity order = buildOrder(orderId, "11.1", null, 0); // 业务订单
            UserEntity user = buildUser(userId, "designer-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);
            when(orderMainMapper.update(any(), any())).thenReturn(1);

            // When
            orderMainService.auditPass(orderId, userId);

            // Then
            verify(orderMainMapper, times(1)).update(any(), any());
            verify(flowFacade, times(1)).executeFlow(any());
        }
    }

    @Test
    void testTrialOrder_DesignManagerAuditPass_WithoutRegionalAudit_ShouldThrowException() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            OrderMainEntity order = buildOrder(orderId, "11.3", 0, 0); // 区域未审核
            UserEntity user = buildUser(userId, "designer-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderMainService.auditPass(orderId, userId));
            assertEquals(ErrorCodeEnum.REGIONAL_AUDIT_PENDING.getCode(), exception.getCode());
        }
    }

    @Test
    void testBusinessOrder_RegionalManagerAudit_ShouldThrowException() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long orderId = 1L;
            Long userId = 100L;
            OrderMainEntity order = buildOrder(orderId, "11.1", null, 0); // 业务订单
            UserEntity user = buildUser(userId, "regional-manager");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(orderMainMapper.selectById(orderId)).thenReturn(order);
            when(userService.getById(userId)).thenReturn(user);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                () -> orderMainService.auditPass(orderId, userId));
            assertEquals(ErrorCodeEnum.NO_AUDIT_PERMISSION.getCode(), exception.getCode());
        }
    }
}
