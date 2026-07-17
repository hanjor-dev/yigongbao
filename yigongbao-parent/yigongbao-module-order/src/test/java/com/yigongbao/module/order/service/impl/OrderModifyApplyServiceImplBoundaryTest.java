package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.entity.OrderMainEntity;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.convert.OrderDiffCalculator;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderModifyFullService;
import com.yigongbao.module.order.utils.OrderModifyTimeWindowChecker;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.module.order.dto.apply.AuditApplyDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderModifyApplyServiceImplBoundaryTest {

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemEntity.class);
        TableInfoHelper.initTableInfo(assistant, OrderFileEntity.class);
        TableInfoHelper.initTableInfo(assistant, OrderModificationApplyEntity.class);
    }

    @Mock private OrderModificationLogMapper logMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper itemMapper;
    @Mock private OrderFileMapper fileMapper;
    @Mock private UserService userService;
    @Mock private FlowFacade flowFacade;
    @Mock private FlowOrderService flowOrderService;
    @Mock private OrderModificationApplyMapper applyMapper;
    @Mock private OrderModifyFullService modifyFullService;
    @Mock private OrderDiffCalculator diffCalculator;
    @Mock private OrderModifyTimeWindowChecker timeWindowChecker;
    @Mock private ConfigService configService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OrderCancelApplyService cancelApplyService;
    @Mock private OrderDataValidator validator;

    @InjectMocks
    private OrderModifyApplyServiceImpl service;

    @Test
    void submitApply_rejectsNullPayload() {
        UserEntity user = new UserEntity();
        user.setRoleCode("BUSINESS_USER");
        when(userService.getById(1L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                    () -> service.submitApply(9L, null));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.INVALID_PARAMETER.getCode());
        }
    }

    @Test
    void submitApply_rejectsMissingOrder() {
        UserEntity user = new UserEntity();
        user.setRoleCode("BUSINESS_USER");
        when(userService.getById(1L)).thenReturn(user);
        when(orderMainMapper.selectById(9L)).thenReturn(null);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                    () -> service.submitApply(9L, new OrderModifyFullDTO()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_NOT_FOUND.getCode());
        }
    }

    @Test
    void auditApply_rejectsNonManager() {
        UserEntity user = new UserEntity();
        user.setRoleCode(RoleCodeEnum.DESIGNER.getCode());
        when(userService.getById(1L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                    () -> service.auditApply(9L, new AuditApplyDTO()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_MODIFY_AUDIT_NO_PERMISSION.getCode());
        }
    }

    @Test
    void submitApply_success_persistsDiffAndPublishesEvent() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRealName("业务员");
        user.setRoleCode("BUSINESS_USER");
        OrderMainEntity order = new OrderMainEntity();
        order.setId(9L);
        order.setOrderCode("ORD-9");
        order.setOperatorId(1L);
        when(userService.getById(1L)).thenReturn(user);
        when(orderMainMapper.selectById(9L)).thenReturn(order);
        when(cancelApplyService.hasPendingCancelApply(9L)).thenReturn(false);
        when(applyMapper.selectList(any())).thenReturn(java.util.List.of());
        when(itemMapper.selectList(any())).thenReturn(java.util.List.of());
        when(fileMapper.selectList(any())).thenReturn(java.util.List.of());
        when(diffCalculator.calculateDiff(any(OrderDraftEntity.class), any(), any(), any()))
                .thenReturn(new com.yigongbao.module.order.dto.diff.OrderModificationDiff());
        when(configService.getConfigValue(any())).thenReturn("20");
        doAnswer(invocation -> {
            OrderModificationApplyEntity apply = invocation.getArgument(0);
            apply.setId(88L);
            return 1;
        }).when(applyMapper).insert(any(OrderModificationApplyEntity.class));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            Long applyId = service.submitApply(9L, new OrderModifyFullDTO());
            assertThat(applyId).isEqualTo(88L);
        }

        verify(applyMapper).insert(any(OrderModificationApplyEntity.class));
        verify(diffCalculator).calculateDiff(any(OrderDraftEntity.class), any(), any(), any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void auditApply_rejection_updatesApplicationAndPublishesNotifications() {
        UserEntity manager = new UserEntity();
        manager.setId(2L);
        manager.setRealName("设计管理员");
        manager.setRoleCode(RoleCodeEnum.DESIGNER_MANAGER.getCode());
        OrderModificationApplyEntity apply = new OrderModificationApplyEntity();
        apply.setId(9L);
        apply.setOrderId(19L);
        apply.setApplyUserId(1L);
        apply.setApplyUserName("业务员");
        apply.setStatus(ApplyStatusEnum.PENDING.getCode());
        apply.setExpireTime(java.time.LocalDateTime.now().plusHours(1));
        OrderMainEntity order = new OrderMainEntity();
        order.setId(19L);
        order.setOrderCode("ORD-19");
        order.setOperatorId(1L);

        when(userService.getById(2L)).thenReturn(manager);
        when(applyMapper.selectById(9L)).thenReturn(apply);
        when(orderMainMapper.selectById(19L)).thenReturn(order);
        AuditApplyDTO dto = new AuditApplyDTO();
        dto.setResult(ApplyStatusEnum.REJECTED.getCode());
        dto.setRemark("资料不完整");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            service.auditApply(9L, dto);
        }

        assertThat(apply.getStatus()).isEqualTo(ApplyStatusEnum.REJECTED.getCode());
        assertThat(apply.getAuditRemark()).isEqualTo("资料不完整");
        assertThat(apply.getAuditUserId()).isEqualTo(2L);
        verify(applyMapper).updateById(apply);
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void auditApply_approvalResetsOnlyDesignAuditStatus() {
        UserEntity manager = new UserEntity();
        manager.setId(2L);
        manager.setRealName("设计管理员");
        manager.setRoleCode(RoleCodeEnum.DESIGNER_MANAGER.getCode());
        UserEntity applicant = new UserEntity();
        applicant.setRoleCode("BUSINESS_USER");

        OrderModificationApplyEntity apply = new OrderModificationApplyEntity();
        apply.setId(10L);
        apply.setOrderId(20L);
        apply.setApplyUserId(1L);
        apply.setApplyUserName("业务员");
        apply.setStatus(ApplyStatusEnum.PENDING.getCode());
        apply.setExpireTime(java.time.LocalDateTime.now().plusHours(1));
        apply.setModificationContent("{}");

        OrderMainEntity order = new OrderMainEntity();
        order.setId(20L);
        order.setPhase(com.yigongbao.flow.enums.FlowPhaseEnum.ORDER.getValue());

        when(userService.getById(2L)).thenReturn(manager);
        when(userService.getById(1L)).thenReturn(applicant);
        when(applyMapper.selectById(10L)).thenReturn(apply);
        when(orderMainMapper.selectById(20L)).thenReturn(order);
        AuditApplyDTO dto = new AuditApplyDTO();
        dto.setResult(ApplyStatusEnum.APPROVED.getCode());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            service.auditApply(10L, dto);
        }

        ArgumentCaptor<LambdaUpdateWrapper<OrderMainEntity>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(orderMainMapper).update(any(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).doesNotContain("regionalAuditStatus");
        assertThat(captor.getValue().getSqlSet()).contains("designAuditStatus");
    }
}
