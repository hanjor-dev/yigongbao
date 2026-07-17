package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.event.CancelApplyApprovedEvent;
import com.yigongbao.common.event.CancelApplyRejectedEvent;
import com.yigongbao.common.event.CancelApplySubmittedEvent;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.order.convert.OrderCancelApplyConvert;
import com.yigongbao.module.order.dto.order.AuditCancelApplyDTO;
import com.yigongbao.module.order.dto.order.CancelOrderApplyDTO;
import com.yigongbao.module.order.dto.order.CancelApplyPageQueryDTO;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.module.order.mapper.OrderCancelApplyMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderCancelApplyService 单元测试
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCancelApplyServiceImplTest {

    @Mock
    private OrderCancelApplyMapper cancelApplyMapper;

    @Mock
    private OrderMainService orderMainService;

    @Mock
    private FlowFacade flowFacade;

    @Mock
    private OrderCancelApplyConvert cancelApplyConvert;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderCancelApplyServiceImpl cancelApplyService;

    private static final Long CURRENT_USER_ID = 1001L;
    private static final Long ORDER_ID = 2001L;
    private static final Long APPLY_ID = 3001L;
    private static final String APPLY_REASON = "客户取消订单";
    private static final String AUDIT_REASON = "审核通过";

    @Test
    void applyStatusCodes_MatchDatabaseContract() {
        assertEquals(1, ApplyStatusEnum.PENDING.getCode());
        assertEquals(2, ApplyStatusEnum.APPROVED.getCode());
        assertEquals(3, ApplyStatusEnum.REJECTED.getCode());
    }

    @Test
    void listPendingApplies_passesOrderCodeFilterToMapper() {
        CancelApplyPageQueryDTO query = new CancelApplyPageQueryDTO();
        query.setOrderCode("ORD-20260717");
        query.setApplyBy(1001L);
        query.setPageNum(1);
        query.setPageSize(20);

        when(userService.getCurrentUserRoleCode()).thenReturn(RoleCodeConstants.DESIGN_ADMIN);
        when(cancelApplyMapper.selectPendingPage(any(Page.class), eq(query)))
                .thenReturn(new Page<>());

        cancelApplyService.listPendingApplies(query);

        verify(cancelApplyMapper).selectPendingPage(any(Page.class), eq(query));
    }

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（继承 ServiceImpl 时必须）
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(cancelApplyService, cancelApplyMapper);

        // 初始化 OrderMainEntity 的表信息（支持 LambdaUpdateWrapper）
        if (TableInfoHelper.getTableInfo(OrderMainEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
        }
        if (TableInfoHelper.getTableInfo(OrderCancelApplyEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, OrderCancelApplyEntity.class);
        }
    }

    /**
     * 测试提交取消申请 - 成功场景
     */
    @Test
    void submitCancelApply_Success() {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason(APPLY_REASON);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.NO);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);

            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(cancelApplyMapper.insert(any(OrderCancelApplyEntity.class))).thenAnswer(invocation -> {
                OrderCancelApplyEntity entity = invocation.getArgument(0);
                entity.setId(APPLY_ID);
                return 1;
            });
            when(orderMainService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

            // Act
            Long applyId = cancelApplyService.submitCancelApply(dto);

            // Assert
            assertNotNull(applyId);
            assertEquals(APPLY_ID, applyId);

            verify(cancelApplyMapper, times(1)).insert(any(OrderCancelApplyEntity.class));
            verify(orderMainService, times(1)).update(any(LambdaUpdateWrapper.class));
            verify(eventPublisher, times(1)).publishEvent(any(CancelApplySubmittedEvent.class));
        }
    }

    /**
     * 测试提交取消申请 - 订单不存在
     */
    @Test
    void submitCancelApply_OrderNotFound() {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason(APPLY_REASON);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());

            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    /**
     * 测试提交取消申请 - 订单已取消
     */
    @Test
    void submitCancelApply_OrderAlreadyCancelled() {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason(APPLY_REASON);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 90, 9010, StatusConstants.NO);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_ALREADY_CANCELLED.getCode(), exception.getCode());

            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    /**
     * 测试提交取消申请 - 已存在待审核申请
     */
    @Test
    void submitCancelApply_HasPendingApply() {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason(APPLY_REASON);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.YES);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING.getCode(), exception.getCode());

            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    /**
     * 测试提交取消申请 - 订单阶段不允许
     */
    @Test
    void submitCancelApply_PhaseNotAllow() {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason(APPLY_REASON);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 10, 1020, StatusConstants.NO);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_PHASE_NOT_ALLOW_APPLY.getCode(), exception.getCode());

            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    void submitCancelApply_CompletedOrderRejected() {
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 80, 8010, StatusConstants.NO);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_PHASE_NOT_ALLOW_APPLY.getCode(), exception.getCode());
            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
        }
    }

    @Test
    void submitCancelApply_ConcurrentPendingClaimRejected() {
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);

        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.NO);
        order.setCreateBy(CURRENT_USER_ID);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(orderMainService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.submitCancelApply(dto));

            assertEquals(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING.getCode(), exception.getCode());
            verify(cancelApplyMapper, never()).insert(any(OrderCancelApplyEntity.class));
        }
    }

    /**
     * 构建订单实体
     */
    private OrderMainEntity buildOrderEntity(Long orderId, Integer phase, Integer status, Integer hasPendingCancelApply) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setPhase(phase);
        order.setStatus(status);
        order.setHasPendingCancelApply(hasPendingCancelApply);
        order.setOrderCode("ORD2026071000001");
        return order;
    }

    /**
     * 测试审核取消申请 - 审核通过
     */
    @Test
    void auditCancelApply_Approved() {
        // Arrange
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(true);
        dto.setReason(AUDIT_REASON);

        OrderCancelApplyEntity apply = buildCancelApplyEntity(APPLY_ID, ORDER_ID, ApplyStatusEnum.PENDING.getCode());
        apply.setApplyBy(7777L);
        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.YES);
        UserEntity auditor = new UserEntity();
        auditor.setId(CURRENT_USER_ID);
        auditor.setRealName("审核员");

        TransitionResult transitionResult = new TransitionResult();
        transitionResult.setTargetPhase(90);
        transitionResult.setTargetStatus(9010);
        transitionResult.setInitialStatus(9010);
        transitionResult.setPhaseChanged(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);

            when(userService.getCurrentUserRoleCode()).thenReturn(RoleCodeConstants.DESIGN_ADMIN);
            when(cancelApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(userService.getById(CURRENT_USER_ID)).thenReturn(auditor);
            when(flowFacade.executeFlow(eq(ORDER_ID), eq(FlowActionEnum.CANCEL), any(FlowOperator.class)))
                    .thenReturn(transitionResult);
            when(cancelApplyMapper.update(any(), any())).thenReturn(1);
            when(orderMainService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);
            when(cancelApplyMapper.updateById(any(OrderCancelApplyEntity.class))).thenReturn(1);

            // Act
            cancelApplyService.auditCancelApply(APPLY_ID, dto);

            // Assert
            verify(flowFacade, times(1)).executeFlow(eq(ORDER_ID), eq(FlowActionEnum.CANCEL), any(FlowOperator.class));
            verify(orderMainService, times(1)).update(any(LambdaUpdateWrapper.class));
            verify(cancelApplyMapper, never()).updateById(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, times(1)).publishEvent(any(CancelApplyApprovedEvent.class));
        }
    }

    /**
     * 测试审核取消申请 - 审核驳回
     */
    @Test
    void auditCancelApply_Rejected() {
        // Arrange
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(false);
        dto.setReason("不符合取消条件");

        OrderCancelApplyEntity apply = buildCancelApplyEntity(APPLY_ID, ORDER_ID, ApplyStatusEnum.PENDING.getCode());
        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.YES);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);

            when(userService.getCurrentUserRoleCode()).thenReturn(RoleCodeConstants.DESIGN_ADMIN);
            when(cancelApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(cancelApplyMapper.update(any(), any())).thenReturn(1);
            when(cancelApplyMapper.updateById(any(OrderCancelApplyEntity.class))).thenReturn(1);
            when(orderMainService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

            // Act
            cancelApplyService.auditCancelApply(APPLY_ID, dto);

            // Assert
            verify(flowFacade, never()).executeFlow(any(), any(), any());
            verify(cancelApplyMapper, never()).updateById(any(OrderCancelApplyEntity.class));
            verify(orderMainService, times(1)).update(any(LambdaUpdateWrapper.class));
            verify(eventPublisher, times(1)).publishEvent(any(CancelApplyRejectedEvent.class));
        }
    }

    @Test
    void auditCancelApply_OrderUpdateFailureRollsBack() {
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(true);

        OrderCancelApplyEntity apply = buildCancelApplyEntity(APPLY_ID, ORDER_ID, ApplyStatusEnum.PENDING.getCode());
        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.YES);
        order.setVersion(4);

        TransitionResult transitionResult = new TransitionResult();
        transitionResult.setTargetPhase(90);
        transitionResult.setTargetStatus(9010);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(userService.getCurrentUserRoleCode()).thenReturn(RoleCodeConstants.DESIGN_ADMIN);
            when(cancelApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(flowFacade.executeFlow(eq(ORDER_ID), eq(FlowActionEnum.CANCEL), any(FlowOperator.class)))
                    .thenReturn(transitionResult);
            when(cancelApplyMapper.update(any(), any())).thenReturn(1);
            when(orderMainService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.auditCancelApply(APPLY_ID, dto));

            assertEquals(ErrorCodeEnum.ORDER_VERSION_CONFLICT.getCode(), exception.getCode());
            verify(cancelApplyMapper, never()).updateById(any(OrderCancelApplyEntity.class));
            verify(eventPublisher, never()).publishEvent(any(CancelApplyApprovedEvent.class));
        }
    }

    @Test
    void auditCancelApply_SecondApprovalRejectedByConditionalUpdate() {
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(true);

        OrderCancelApplyEntity apply = buildCancelApplyEntity(APPLY_ID, ORDER_ID, ApplyStatusEnum.PENDING.getCode());
        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.YES);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(userService.getCurrentUserRoleCode()).thenReturn(RoleCodeConstants.DESIGN_ADMIN);
            when(cancelApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(cancelApplyMapper.update(any(), any())).thenReturn(0);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.auditCancelApply(APPLY_ID, dto));

            assertEquals(ErrorCodeEnum.CANCEL_APPLY_ALREADY_AUDITED.getCode(), exception.getCode());
            verify(flowFacade, never()).executeFlow(any(), any(), any());
        }
    }

    @Test
    void listPendingApplies_RequiresDesignAdmin() {
        CancelApplyPageQueryDTO dto = new CancelApplyPageQueryDTO();
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(userService.getCurrentUserRoleCode()).thenReturn("USER");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.listPendingApplies(dto));

            assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), exception.getCode());
            verify(cancelApplyMapper, never()).selectPage(any(), any());
        }
    }

    @Test
    void getCancelApplyDetail_UnrelatedUserRejected() {
        OrderCancelApplyEntity apply = buildCancelApplyEntity(APPLY_ID, ORDER_ID, ApplyStatusEnum.PENDING.getCode());
        apply.setApplyBy(7777L);
        OrderMainEntity order = buildOrderEntity(ORDER_ID, 20, 2010, StatusConstants.NO);
        order.setCreateBy(9999L);
        order.setDesignerId(8888L);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(CURRENT_USER_ID);
            when(userService.getCurrentUserRoleCode()).thenReturn("USER");
            when(cancelApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> cancelApplyService.getCancelApplyDetail(APPLY_ID));

            assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), exception.getCode());
        }
    }

    /**
     * 构建取消申请实体
     */
    private OrderCancelApplyEntity buildCancelApplyEntity(Long applyId, Long orderId, Integer auditStatus) {
        OrderCancelApplyEntity apply = new OrderCancelApplyEntity();
        apply.setId(applyId);
        apply.setOrderId(orderId);
        apply.setApplyBy(CURRENT_USER_ID);
        apply.setApplyReason(APPLY_REASON);
        apply.setAuditStatus(auditStatus);
        return apply;
    }
}
