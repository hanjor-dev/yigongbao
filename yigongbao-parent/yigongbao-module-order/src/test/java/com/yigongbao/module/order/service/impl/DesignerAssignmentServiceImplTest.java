package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignerAssignmentServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignerAssignmentServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private ConfigService configService;
    @Mock private DictService dictService;
    @Mock private RebuildProjectService rebuildProjectService;
    @Mock private com.yigongbao.module.order.mapper.OrderDesignerAssignmentLogMapper assignmentLogMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private DesignerAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(orderMainService.updateById(any(OrderMainEntity.class))).thenReturn(true);
    }

    // ==================== triggerAssignmentAfterAudit ====================

    @Test
    @DisplayName("trigger — 手动模式，不调用 autoAssign")
    void trigger_manualMode_shouldSkipAutoAssign() {
        when(configService.getConfigValue("design.assign.mode")).thenReturn("manual");
        service.triggerAssignmentAfterAudit(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("trigger — 自动模式，无明细时不抛异常")
    void trigger_autoMode_noItems_shouldNotThrow() {
        when(configService.getConfigValue("design.assign.mode")).thenReturn("auto");
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        assertDoesNotThrow(() -> service.triggerAssignmentAfterAudit(1L));
    }

    @Test
    @DisplayName("trigger — 自动模式分配异常不上抛，订单不阻断")
    void trigger_autoMode_exceptionCaught_shouldNotThrow() {
        when(configService.getConfigValue("design.assign.mode")).thenReturn("auto");
        when(orderItemMapper.selectList(any())).thenThrow(new RuntimeException("DB error"));
        assertDoesNotThrow(() -> service.triggerAssignmentAfterAudit(1L));
    }

    // ==================== autoAssignDesigner ====================

    @Test
    @DisplayName("autoAssign — 找到候选设计师，更新订单 designerId")
    void autoAssign_withCandidate_shouldUpdateOrder() {
        // given
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        UserEntity designer = new UserEntity();
        designer.setId(100L);
        designer.setRealName("张三");
        when(userMapper.selectAvailableDesigners("7.1")).thenReturn(List.of(designer));
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        // when
        Long result = service.autoAssignDesigner(1L);
        // then
        assertEquals(100L, result);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("autoAssign — 精确匹配无结果，通用专业方向兜底成功")
    void autoAssign_fallbackToGeneral_shouldAssign() {
        // given
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        // 精确匹配无结果
        when(userMapper.selectAvailableDesigners("7.1")).thenReturn(List.of());
        // 通用专业方向兜底成功
        UserEntity generalDesigner = new UserEntity();
        generalDesigner.setId(200L);
        generalDesigner.setRealName("通用设计师");
        when(userMapper.selectAvailableDesigners("7.99")).thenReturn(List.of(generalDesigner));
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        // when
        Long result = service.autoAssignDesigner(1L);
        // then
        assertEquals(200L, result);
        verify(userMapper).selectAvailableDesigners("7.1"); // 第一次查询
        verify(userMapper).selectAvailableDesigners("7.99"); // 兜底查询
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(200L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("autoAssign — 精确匹配和通用兜底均无结果，返回 null")
    void autoAssign_noCandidate_shouldReturnNull() {
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        when(userMapper.selectAvailableDesigners("7.1")).thenReturn(List.of());
        when(userMapper.selectAvailableDesigners("7.99")).thenReturn(List.of());
        assertNull(service.autoAssignDesigner(1L));
    }

    @Test
    @DisplayName("autoAssign — 订单无明细，返回 null")
    void autoAssign_noOrderItems_shouldReturnNull() {
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        assertNull(service.autoAssignDesigner(1L));
    }

    // ==================== manualAssignDesigner ====================

    @Test
    @DisplayName("manualAssign — 正常流程，更新订单设计师")
    void manualAssign_success() {
        OrderMainEntity order = buildPendingDesignOrder(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        UserEntity designer = buildDesigner(100L, "7.1");
        when(userMapper.selectById(100L)).thenReturn(designer);
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of(designer));
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        service.manualAssignDesigner(1L, 100L);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("manualAssign — 订单不存在，抛 ORDER_NOT_FOUND")
    void manualAssign_orderNotFound_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计中状态允许重新分配，并同步当前处理人")
    void manualAssign_designInProgress_shouldReassignAndUpdateHandler() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(200L);
        order.setDesignerName("设计师200");
        order.setCurrentHandlerId(200L);
        order.setCurrentHandlerName("设计师200");
        when(orderMainService.getById(1L)).thenReturn(order);
        UserEntity designer = buildDesigner(100L, "7.1");
        when(userMapper.selectById(100L)).thenReturn(designer);
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of(designer));

        service.manualAssignDesigner(1L, 100L);

        verify(orderMainService).updateById(argThat(updated ->
                Long.valueOf(100L).equals(updated.getDesignerId())
                        && "设计师100".equals(updated.getDesignerName())
                        && Long.valueOf(100L).equals(updated.getCurrentHandlerId())
                        && "设计师100".equals(updated.getCurrentHandlerName())));
    }

    @Test
    @DisplayName("manualAssign — 已是当前设计师时不重复更新")
    void manualAssign_sameDesigner_shouldSkipUpdate() {
        OrderMainEntity order = buildPendingDesignOrder(1L);
        order.setDesignerId(100L);
        when(orderMainService.getById(1L)).thenReturn(order);

        service.manualAssignDesigner(1L, 100L);

        verify(orderMainService, never()).updateById(any(OrderMainEntity.class));
        verifyNoInteractions(userMapper, assignmentLogMapper, eventPublisher);
    }

    @Test
    @DisplayName("manualAssign — 设计师不存在，抛 DESIGNER_NOT_FOUND")
    void manualAssign_designerNotFound_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        when(userMapper.selectById(100L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师已被逻辑删除，抛 DESIGNER_NOT_FOUND")
    void manualAssign_designerDeleted_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setIsDeleted(1);
        when(userMapper.selectById(100L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师角色不合法，抛 DESIGNER_ROLE_INVALID")
    void manualAssign_wrongRole_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRoleCode("sales");
        user.setStatus(1);
        user.setIsDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_ROLE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师已禁用，抛 DESIGNER_DISABLED")
    void manualAssign_designerDisabled_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRoleCode("designer");
        user.setStatus(0);
        user.setIsDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 专业方向不匹配时，当前版本仍允许已授权设计师分配")
    void manualAssign_specialtyMismatch_shouldStillAssign_whenPermissionGranted() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity designer = buildDesigner(100L, "7.2"); // 设计师是 7.2
        when(userMapper.selectById(100L)).thenReturn(designer);
        // 权限校验通过（设计师在权限列表中）
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of(designer));
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1"); // 订单是 7.1
        service.manualAssignDesigner(1L, 100L);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("manualAssign — 订单无专业方向，跳过 specialty 校验，分配成功")
    void manualAssign_noOrderSpecialty_shouldSkipSpecialtyCheck() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity designer = buildDesigner(100L, "7.1");
        when(userMapper.selectById(100L)).thenReturn(designer);
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of(designer));
        // 订单无明细 → getOrderSpecialty 返回 null → 跳过 specialty 校验
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        service.manualAssignDesigner(1L, 100L);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    // ==================== listAvailableDesigners ====================

    @Test
    @DisplayName("listAvailableDesigners — specialties 为空，仍返回所有设计师")
    void listDesigners_emptySpecialties_shouldReturnEmpty() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of());
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of());
        List<DesignerVO> result = service.listAvailableDesigners(dto);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listAvailableDesigners — 非法 specialty 编码，仍返回所有设计师（不按 specialty 过滤）")
    void listDesigners_invalidSpecialty_shouldReturnEmpty() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of("invalid", "7.", "../hack"));
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of());
        List<DesignerVO> result = service.listAvailableDesigners(dto);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listAvailableDesigners — 返回设计师列表")
    void listDesigners_validSpecialty_shouldReturnList() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of("7.1"));
        UserEntity designer = buildDesigner(100L, "7.1");
        designer.setCurrentLoad(3);
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of(designer));
        List<DesignerVO> result = service.listAvailableDesigners(dto);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getUserId());
        assertEquals(3, result.get(0).getCurrentLoad());
    }

    @Test
    @DisplayName("listAvailableDesigners — 多个 specialty，调用 selectAllDesignersByPermission")
    void listDesigners_multipleSpecialties_shouldBuildOrCondition() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of("7.1", "7.2"));
        when(userMapper.selectAllDesignersByPermission(null)).thenReturn(List.of());
        service.listAvailableDesigners(dto);
        verify(userMapper).selectAllDesignersByPermission(null);
    }

    // ==================== 辅助方法 ====================

    private OrderMainEntity buildPendingDesignOrder(Long id) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue());
        return order;
    }

    private UserEntity buildDesigner(Long id, String specialty) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRealName("设计师" + id);
        user.setRoleCode("designer");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setSpecialty(specialty);
        return user;
    }
}
