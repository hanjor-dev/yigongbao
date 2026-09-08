package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.convert.OrderConvert;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.PublicOrderCodeGenerator;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderMainServiceImplStateTransitionTest {

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderMainEntity.class);
    }

    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderDraftMapper orderDraftMapper;
    @Mock private OrderItemDraftMapper orderItemDraftMapper;
    @Mock private OrderFileMapper orderFileMapper;
    @Mock private OrderModificationLogMapper orderModificationLogMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private PublicOrderCodeGenerator publicOrderCodeGenerator;
    @Mock private FileService fileService;
    @Mock private OrgService orgService;
    @Mock private FlowFacade flowFacade;
    @Mock private ConfigService configService;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private com.yigongbao.module.order.helper.OrderQueryHelper orderQueryHelper;
    @Mock private ObjectMapper objectMapper;
    @Mock private OrderDataValidator orderDataValidator;
    @Mock private com.yigongbao.module.order.validator.OrderDataScopeChecker orderDataScopeChecker;
    @Mock private OrderModifyApplyService orderModifyApplyService;
    @Mock private OrderCancelApplyService cancelApplyService;
    @Mock private OrderConvert orderConvert;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DesignerAssignmentService designerAssignmentService;

    @Spy
    @InjectMocks
    private OrderMainServiceImpl service;

    @Test
    void listAvailableActions_rejectsMissingOrder() {
        doReturn(null).when(service).getById(101L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.listAvailableActions(101L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_NOT_FOUND.getCode());
        verifyNoInteractions(flowFacade);
    }

    @Test
    void getOrderDetail_rejectsMissingOrderWithoutQueryingDependencies() {
        doReturn(null).when(service).getById(100L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.getOrderDetail(100L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_NOT_FOUND.getCode());
        verifyNoInteractions(orderItemMapper, flowFacade, orderFileMapper, orderConvert);
    }

    @Test
    void listAvailableActions_delegatesExistingOrderToFlowFacade() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(102L);
        doReturn(order).when(service).getById(102L);
        when(flowFacade.getAvailableActions(102L)).thenReturn(java.util.List.of("SUBMIT"));

        var actions = service.listAvailableActions(102L);

        assertThat(actions).containsExactly("SUBMIT");
        verify(flowFacade).getAvailableActions(102L);
    }

    @Test
    void checkNotClassicCase_rejectsProtectedOrder() {
        OrderMainEntity order = new OrderMainEntity();
        order.setIsClassicCase(1);
        doReturn(order).when(service).getById(103L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.checkNotClassicCase(103L, "修改"));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.CLASSIC_CASE_PROTECTED.getCode());
    }

    @Test
    void checkNotClassicCase_allowsMissingOrNormalOrder() {
        doReturn(null).when(service).getById(104L);
        service.checkNotClassicCase(104L, "修改");

        OrderMainEntity order = new OrderMainEntity();
        order.setIsClassicCase(0);
        doReturn(order).when(service).getById(105L);
        service.checkNotClassicCase(105L, "修改");
    }

    @Test
    void updateOrder_allowsPhysicalDeliveryChangeFromZeroToOne() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(106L);
        order.setPhase(FlowPhaseEnum.ORDER.getValue());
        order.setNeedsPhysicalDelivery(0);
        when(userHospitalService.getDataScopeType(anyLong())).thenReturn(DataScopeTypeEnum.ALL);
        when(orderQueryHelper.getCurrentUserId()).thenReturn(11L);
        doReturn(1L).when(service).count(any());
        doReturn(order).when(service).getById(106L);
        doNothing().when(service).checkNotClassicCase(106L, "修改");
        doReturn(true).when(service).updateById(order);

        UpdateOrderDTO dto = new UpdateOrderDTO();
        dto.setNeedsPhysicalDelivery(1);
        dto.setPatientName("更新后");

        service.updateOrder(106L, dto);

        assertThat(order.getNeedsPhysicalDelivery()).isEqualTo(1);
        assertThat(order.getPatientName()).isEqualTo("更新后");
        verify(service).updateById(order);
    }

    @Test
    void updateOrder_rejectsPhysicalDeliveryChangeOutsideOrderPhase() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(107L);
        order.setPhase(FlowPhaseEnum.DESIGN.getValue());
        order.setNeedsPhysicalDelivery(0);
        when(userHospitalService.getDataScopeType(anyLong())).thenReturn(DataScopeTypeEnum.ALL);
        when(orderQueryHelper.getCurrentUserId()).thenReturn(11L);
        doReturn(1L).when(service).count(any());
        doReturn(order).when(service).getById(107L);
        doNothing().when(service).checkNotClassicCase(107L, "修改");

        UpdateOrderDTO dto = new UpdateOrderDTO();
        dto.setNeedsPhysicalDelivery(1);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.updateOrder(107L, dto));

        assertThat(exception.getCode())
                .isEqualTo(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN.getCode());
        verify(service, never()).updateById(any());
    }

    @Test
    void removeOrder_requiresOwnedDraftAndDeletesRelatedRows() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(108L);
        order.setCreateBy(11L);
        order.setStatus(FlowStatusEnum.DRAFT.getValue());
        doReturn(order).when(service).getById(108L);
        doNothing().when(service).checkNotClassicCase(108L, "删除");
        when(orderQueryHelper.getCurrentUserId()).thenReturn(11L);
        doReturn(true).when(service).removeById(108L);

        service.removeOrder(108L);

        verify(service).removeById(108L);
        verify(orderItemMapper).delete(any());
        verify(orderFileMapper).delete(any());
        verify(orderModificationLogMapper).delete(any());
    }

    @Test
    void directCancel_doesNotWriteStaleOrderAfterOptimisticUpdate() {
        Long orderId = 7L;
        Long userId = 11L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setCreateBy(userId);
        order.setPhase(FlowPhaseEnum.ORDER.getValue());
        order.setStatus(2010);
        order.setIsClassicCase(0);

        TransitionResult transition = TransitionResult.of(FlowPhaseEnum.ORDER.getValue(), 9010);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doReturn(order).when(service).getById(orderId);
            doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
            doReturn(true).when(service).updateById(any(OrderMainEntity.class));
            when(userService.getById(userId)).thenReturn(null);
            when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.CANCEL), any(), eq(3)))
                    .thenReturn(transition);

            service.cancelOrder(orderId, 3);

            verify(service).update(any(LambdaUpdateWrapper.class));
            verify(service, never()).updateById(any(OrderMainEntity.class));
            verify(eventPublisher).publishEvent(any());
        }
    }

    @Test
    void submitOrder_persistsFlowTargetAndPublishesSubmittedEvent() {
        Long orderId = 8L;
        Long userId = 11L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setCreateBy(userId);
        TransitionResult transition = TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
        when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.SUBMIT_ORDER), any()))
                .thenReturn(transition);
        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        doReturn(order).when(service).getById(orderId);
        doReturn(true).when(service).updateById(any(OrderMainEntity.class));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            service.submitOrder(orderId);
        }

        verify(service).updateById(order);
        verify(eventPublisher).publishEvent(any());
        org.junit.jupiter.api.Assertions.assertEquals(FlowStatusEnum.PENDING_DATA_AUDIT.getValue(), order.getStatus());
    }

    @Test
    void withdrawOrder_updatesTargetAndCurrentHandler() {
        Long orderId = 9L;
        Long userId = 11L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setCreateBy(userId);
        TransitionResult transition = TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                FlowStatusEnum.DRAFT.getValue());
        when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.WITHDRAW), any())).thenReturn(transition);
        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        doReturn(order).when(service).getById(orderId);
        doReturn(true).when(service).updateById(any(OrderMainEntity.class));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            service.withdrawOrder(orderId);
        }

        verify(service).updateById(order);
        org.junit.jupiter.api.Assertions.assertEquals(userId, order.getCurrentHandlerId());
    }

    @Test
    void manualCompleteOrder_requiresDesignCompletedNonDeliveryOrder() {
        Long orderId = 10L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setNeedsPhysicalDelivery(0);
        order.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        doReturn(order).when(service).getById(orderId);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        ReflectionTestUtils.setField(service, "designerAssignmentService", designerAssignmentService);
        when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.COMPLETE), any(), eq(1)))
                .thenReturn(TransitionResult.of(FlowPhaseEnum.ORDER.getValue(), FlowStatusEnum.COMPLETED.getValue()));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            service.manualCompleteOrder(orderId, 1);
        }

        verify(service).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void auditPass_updatesPendingDesignAuditAndTriggersAssignment() {
        Long orderId = 12L;
        Long userId = 11L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setOrderCode("ORD-12");
        order.setDesignAuditStatus(com.yigongbao.common.constant.AuditStatusConstants.PENDING);
        TransitionResult transition = TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                FlowStatusEnum.PENDING_DESIGN.getValue());
        com.yigongbao.module.order.dto.order.AuditOrderDTO dto =
                new com.yigongbao.module.order.dto.order.AuditOrderDTO();
        dto.setVersion(1);
        dto.setRemark("通过");
        when(orderQueryHelper.getCurrentUserRoleCode()).thenReturn(
                com.yigongbao.common.constant.RoleCodeConstants.DESIGN_ADMIN);
        when(orderModifyApplyService.hasPendingApply(orderId)).thenReturn(false);
        when(cancelApplyService.hasPendingCancelApply(orderId)).thenReturn(false);
        when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.DATA_AUDIT_PASS), any(), eq(1)))
                .thenReturn(transition);
        when(userService.getById(userId)).thenReturn(null);
        doReturn(order).when(service).getById(orderId);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        ReflectionTestUtils.setField(service, "designerAssignmentService", designerAssignmentService);

        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            service.auditPass(orderId, dto);
        }

        verify(service).update(any(LambdaUpdateWrapper.class));
        verify(designerAssignmentService).triggerAssignmentAfterAudit(orderId);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void auditReject_requiresRemarkBeforeFlowExecution() {
        Long orderId = 13L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        doReturn(order).when(service).getById(orderId);
        com.yigongbao.module.order.dto.order.AuditOrderDTO dto =
                new com.yigongbao.module.order.dto.order.AuditOrderDTO();
        dto.setRemark(" ");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.yigongbao.common.exception.BusinessException.class,
                () -> service.auditReject(orderId, dto));
        verifyNoInteractions(flowFacade);
    }

    @Test
    void createFromDraft_copiesDraftAndCreatesPendingAuditOrder() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setId(30L);
        draft.setOperatorId(11L);
        draft.setOrgId(101L);
        draft.setOrderType(1);
        draft.setBusinessType("business");
        when(codeGeneratorService.generate(any())).thenReturn("ORD-30");
        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setOrgId(101L);
        user.setRealName("草稿操作员");
        when(userService.getById(11L)).thenReturn(user);
        when(orderItemDraftMapper.selectList(any())).thenReturn(java.util.List.of());
        when(fileService.listByBiz(anyString(), anyLong())).thenReturn(java.util.List.of());
        doNothing().when(orderDataValidator).validateOrderType(eq(11L), eq(1));
        doAnswer(invocation -> {
            OrderMainEntity created = invocation.getArgument(0);
            created.setId(300L);
            return true;
        }).when(service).save(any(OrderMainEntity.class));
        when(flowFacade.executeFlow(eq(300L), eq(FlowActionEnum.CREATE), any()))
                .thenReturn(TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                        FlowStatusEnum.PENDING_DATA_AUDIT.getValue()));

        Long orderId = service.createFromDraft(draft);

        org.junit.jupiter.api.Assertions.assertEquals(300L, orderId);
        verify(service).save(argThat(order ->
                "ORD-30".equals(order.getOrderCode())
                        && FlowPhaseEnum.ORDER.getValue().equals(order.getPhase())
                        && FlowStatusEnum.PENDING_DATA_AUDIT.getValue().equals(order.getStatus())));
        verify(flowFacade).executeFlow(eq(300L), eq(FlowActionEnum.CREATE), any());
    }

    @Test
    void createFromDraft_trialOrderDoesNotInitializeRegionalAudit() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setId(31L);
        draft.setOperatorId(11L);
        draft.setOrgId(102L);
        draft.setOrderType(1);
        draft.setBusinessType("11.3");
        when(codeGeneratorService.generate(any())).thenReturn("ORD-31");
        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setOrgId(102L);
        user.setRealName("草稿操作员");
        when(userService.getById(11L)).thenReturn(user);
        when(orderItemDraftMapper.selectList(any())).thenReturn(java.util.List.of());
        when(fileService.listByBiz(anyString(), anyLong())).thenReturn(java.util.List.of());
        doNothing().when(orderDataValidator).validateOrderType(eq(11L), eq(1));
        doAnswer(invocation -> {
            OrderMainEntity created = invocation.getArgument(0);
            created.setId(301L);
            return true;
        }).when(service).save(any(OrderMainEntity.class));
        when(flowFacade.executeFlow(eq(301L), eq(FlowActionEnum.CREATE), any()))
                .thenReturn(TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                        FlowStatusEnum.PENDING_DATA_AUDIT.getValue()));

        service.createFromDraft(draft);

        verify(service).save(argThat(order ->
                order.getRegionalAuditStatus() == null
                        && order.getDesignAuditStatus() != null
                        && order.getDesignAuditStatus() == com.yigongbao.common.constant.AuditStatusConstants.PENDING));
    }

    @Test
    void createFromDraft_rejectsDraftFromAnotherOrganization() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setId(32L);
        draft.setOperatorId(11L);
        draft.setOrgId(202L);

        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setOrgId(101L);
        when(userService.getById(11L)).thenReturn(user);
        when(codeGeneratorService.generate(any())).thenReturn("ORD-CROSS-DRAFT");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.yigongbao.common.exception.BusinessException.class,
                () -> service.createFromDraft(draft));

        verify(service, never()).save(any(OrderMainEntity.class));
        verifyNoInteractions(flowFacade, orderItemMapper, orderItemDraftMapper);
    }

    @Test
    void createFromDraft_rejectsHospitalOutsideOperatorsCurrentScope() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setId(33L);
        draft.setOperatorId(11L);
        draft.setOrgId(101L);
        draft.setHospitalId(501L);

        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setOrgId(101L);
        when(userService.getById(11L)).thenReturn(user);
        when(codeGeneratorService.generate(any())).thenReturn("ORD-HOSPITAL-DENIED");
        when(userHospitalService.hasPermissionOnHospital(11L, 501L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createFromDraft(draft));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.HOSPITAL_SCOPE_DENIED.getCode());
        verify(service, never()).save(any(OrderMainEntity.class));
    }

    @Test
    void resubmit_legacyRegionalRejectedOrderUsesDesignAuditAsCurrentGate() {
        Long orderId = 32L;
        Long userId = 11L;
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setCreateBy(userId);
        order.setStatus(FlowStatusEnum.DATA_AUDIT_REJECTED.getValue());
        order.setRegionalAuditStatus(com.yigongbao.common.constant.AuditStatusConstants.REJECTED);
        order.setDesignAuditStatus(com.yigongbao.common.constant.AuditStatusConstants.PENDING);

        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(null);
        doReturn(order).when(service).getById(orderId);
        when(flowFacade.executeFlow(eq(orderId), eq(FlowActionEnum.RESUBMIT), any(), eq(3)))
                .thenReturn(TransitionResult.of(FlowPhaseEnum.ORDER.getValue(),
                        FlowStatusEnum.PENDING_DATA_AUDIT.getValue()));
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        service.resubmit(orderId, 3);

        ArgumentCaptor<LambdaUpdateWrapper<OrderMainEntity>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(service).update(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).doesNotContain("designAuditStatus");
        assertThat(captor.getValue().getSqlSegment()).contains("status");
    }

    @Test
    void createOrder_rejectsWhenCurrentUserDoesNotExist() {
        com.yigongbao.module.order.dto.order.CreateOrderDTO dto = new com.yigongbao.module.order.dto.order.CreateOrderDTO();
        when(orderQueryHelper.getCurrentUserId()).thenReturn(11L);
        when(userService.getById(11L)).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.yigongbao.common.exception.BusinessException.class,
                () -> service.createOrder(dto));

        verify(service, never()).save(any(OrderMainEntity.class));
        verifyNoInteractions(flowFacade, eventPublisher);
    }

    @Test
    void createOrder_rejectsOrderFromAnotherOrganization() {
        Long userId = 11L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setOrgId(101L);

        com.yigongbao.module.order.dto.order.CreateOrderDTO dto = new com.yigongbao.module.order.dto.order.CreateOrderDTO();
        dto.setOrgId(202L);

        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(user);
        when(codeGeneratorService.generate(any())).thenReturn("ORD-CROSS-ORG");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.yigongbao.common.exception.BusinessException.class,
                () -> service.createOrder(dto));

        verify(service, never()).save(any(OrderMainEntity.class));
        verifyNoInteractions(orderDataValidator, flowFacade, eventPublisher);
    }

    @Test
    void createOrder_buildsPendingOrderAndRecordsCreateFlow() {
        Long userId = 11L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRealName("操作员");
        user.setPhone("13800000000");
        user.setOrgId(101L);
        user.setDeptId(21L);
        user.setDeptName("影像科");

        com.yigongbao.module.order.dto.order.CreateOrderDTO dto = new com.yigongbao.module.order.dto.order.CreateOrderDTO();
        dto.setOrderType(1);
        dto.setNeedsPhysicalDelivery(0);
        dto.setBusinessType("11.1");
        dto.setOrgId(101L);
        dto.setHospitalId(201L);
        dto.setHospitalDeptId(301L);
        dto.setPatientName("患者甲");
        dto.setItems(java.util.List.of());

        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        when(codeGeneratorService.generate(any())).thenReturn("ORD-100");
        when(publicOrderCodeGenerator.generate()).thenReturn("YG23456789AB");
        when(configService.getConfigValue(any())).thenReturn("false");
        when(userService.getById(userId)).thenReturn(user);
        doAnswer(invocation -> {
            OrderMainEntity order = invocation.getArgument(0);
            order.setId(100L);
            return true;
        }).when(service).save(any(OrderMainEntity.class));

        Long orderId = service.createOrder(dto);

        org.junit.jupiter.api.Assertions.assertEquals(100L, orderId);
        verify(service).save(argThat(order ->
                "ORD-100".equals(order.getOrderCode())
                        && "YG23456789AB".equals(order.getPublicOrderCode())
                        && FlowPhaseEnum.ORDER.getValue().equals(order.getPhase())
                        && FlowStatusEnum.PENDING_DATA_AUDIT.getValue().equals(order.getStatus())
                        && userId.equals(order.getOperatorId())
                        && "操作员".equals(order.getOperatorName())));
        verify(orderDataValidator).validateAndFillMasterForOrder(any(), eq(101L), eq(201L), eq(301L),
                isNull(), isNull(), isNull(), eq(userId), eq(OrderDataValidator.ValidateMode.DIRECT));
        verify(flowFacade).executeFlow(eq(100L), eq(FlowActionEnum.CREATE), any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createOrder_trialOrderUsesOnlyDesignAudit() {
        Long userId = 12L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRealName("试用订单操作员");
        user.setPhone("13800000001");
        user.setOrgId(102L);
        user.setDeptId(22L);
        user.setDeptName("试用科室");

        com.yigongbao.module.order.dto.order.CreateOrderDTO dto = new com.yigongbao.module.order.dto.order.CreateOrderDTO();
        dto.setOrderType(1);
        dto.setNeedsPhysicalDelivery(0);
        dto.setBusinessType("11.3");
        dto.setOrgId(102L);
        dto.setHospitalId(202L);
        dto.setHospitalDeptId(302L);
        dto.setPatientName("试用患者");
        dto.setApprovalFileIds(java.util.List.of("approval-file-1"));
        dto.setItems(java.util.List.of());

        when(orderQueryHelper.getCurrentUserId()).thenReturn(userId);
        when(codeGeneratorService.generate(any())).thenReturn("ORD-TRIAL-100");
        when(configService.getConfigValue(any())).thenReturn("false");
        when(userService.getById(userId)).thenReturn(user);
        FileVO approvalFile = new FileVO();
        approvalFile.setId("approval-file-1");
        when(fileService.listByIds(any())).thenReturn(java.util.List.of(approvalFile));
        doAnswer(invocation -> {
            OrderMainEntity order = invocation.getArgument(0);
            order.setId(101L);
            return true;
        }).when(service).save(any(OrderMainEntity.class));

        Long orderId = service.createOrder(dto);

        org.junit.jupiter.api.Assertions.assertEquals(101L, orderId);
        ArgumentCaptor<OrderMainEntity> orderCaptor = ArgumentCaptor.forClass(OrderMainEntity.class);
        verify(service).save(orderCaptor.capture());
        OrderMainEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getBusinessType()).isEqualTo("11.3");
        assertThat(savedOrder.getStatus()).isEqualTo(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
        assertThat(savedOrder.getDesignAuditStatus()).isEqualTo(0);
        assertThat(savedOrder.getRegionalAuditStatus()).isNull();
        verify(flowFacade).executeFlow(eq(101L), eq(FlowActionEnum.CREATE), any());
        verify(eventPublisher).publishEvent(any());
    }
}
