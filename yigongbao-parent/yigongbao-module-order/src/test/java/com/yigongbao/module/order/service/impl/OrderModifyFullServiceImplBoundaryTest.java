package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.validator.OrderDataScopeChecker;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderModifyFullServiceImplBoundaryTest {

    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderFileMapper orderFileMapper;
    @Mock private OrderModificationLogMapper logMapper;
    @Mock private OrderDataValidator validator;
    @Mock private OrderDataScopeChecker dataScopeChecker;
    @Mock private FlowFacade flowFacade;
    @Mock private OrgService orgService;
    @Mock private HospitalDeptService hospitalDeptService;
    @Mock private UserService userService;
    @Mock private DictService dictService;

    @InjectMocks
    private OrderModifyFullServiceImpl service;

    @Test
    void modifyOrderFull_rejectsMissingOrder() {
        when(orderMainMapper.selectById(7L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.modifyOrderFull(7L, new OrderModifyFullDTO(), true, 1L, "管理员", "ADMIN"));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DATA_NOT_FOUND.getCode());
    }

    @Test
    void modifyOrderFull_rejectsDesignerOutsideDesignPhase() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(7L);
        order.setPhase(10);
        when(orderMainMapper.selectById(7L)).thenReturn(order);
        UserEntity designer = new UserEntity();
        designer.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.DESIGNER.getCode());
        when(userService.getById(1L)).thenReturn(designer);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.modifyOrderFull(7L, new OrderModifyFullDTO(), false, 1L, "设计师",
                            com.yigongbao.common.enums.RoleCodeEnum.DESIGNER.getCode()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED.getCode());
        }
        verifyNoInteractions(flowFacade, validator);
    }

    @Test
    void modifyOrderFull_rejectsDesignerDirectlyInDesignPhase() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(7L);
        order.setPhase(20);
        when(orderMainMapper.selectById(7L)).thenReturn(order);
        UserEntity designer = new UserEntity();
        designer.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.DESIGNER.getCode());
        when(userService.getById(1L)).thenReturn(designer);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.modifyOrderFull(7L, new OrderModifyFullDTO(), false, 1L, "设计师",
                            com.yigongbao.common.enums.RoleCodeEnum.DESIGNER.getCode()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED.getCode());
        }
        verifyNoInteractions(flowFacade, validator);
    }

    @Test
    void modifyOrderFull_rejectsNonBusinessNonAdminRole() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(7L);
        order.setPhase(10);
        when(orderMainMapper.selectById(7L)).thenReturn(order);
        UserEntity productionWorker = new UserEntity();
        productionWorker.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.PRODUCTION_WORKER.getCode());
        when(userService.getById(1L)).thenReturn(productionWorker);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.modifyOrderFull(7L, new OrderModifyFullDTO(), false, 1L, "生产员",
                            com.yigongbao.common.enums.RoleCodeEnum.PRODUCTION_WORKER.getCode()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED.getCode());
        }
    }

    @Test
    void modifyOrderFull_appliedDesignerApplicationCanChangeAllFields() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(7L);
        order.setOrderCode("ORD-7");
        order.setPhase(20);
        order.setVersion(0);
        order.setPatientName("旧患者");
        order.setPatientGender(null);
        order.setPatientAge(20);
        when(orderMainMapper.selectById(7L)).thenReturn(order);

        OrderModifyFullDTO dto = new OrderModifyFullDTO();
        dto.setPatientName("新患者");
        dto.setPatientGender(null);
        dto.setPatientAge(20);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            service.modifyOrderFull(7L, dto, true, 1L, "设计师", "designer");
        }

        assertThat(order.getPatientName()).isEqualTo("新患者");
        verify(orderMainMapper).updateById(order);
    }

    @Test
    void modifyOrderFull_rejectsOrderOutsideCurrentUsersDataScope() {
        doThrow(new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND))
                .when(dataScopeChecker).checkOrderAccess(7L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.modifyOrderFull(7L, new OrderModifyFullDTO(), false, 1L, "区域管理员",
                            com.yigongbao.common.enums.RoleCodeEnum.REGIONAL_MANAGER.getCode()));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_NOT_FOUND.getCode());
        }
        verify(orderMainMapper, never()).selectById(anyLong());
    }
}
