package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IClassicCaseFileService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderClassicCaseServiceImplTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private IClassicCaseFileService classicCaseFileService;
    @Mock
    private OrderMainService orderMainService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OrderQueryHelper orderQueryHelper;
    @Mock
    private com.yigongbao.module.system.user.service.UserService userService;

    @InjectMocks
    private OrderClassicCaseServiceImpl classicCaseService;

    @Test
    void markAsClassicCase_success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
            dto.setOrderId(1L);
            dto.setRemark("优秀案例");

            OrderMainEntity order = new OrderMainEntity();
            order.setId(1L);
            order.setOrderCode("ORD001");
            order.setPhase(80);
            order.setIsClassicCase(StatusConstants.NO);
            order.setNeedsPhysicalDelivery(StatusConstants.NO);

            when(orderMainMapper.selectById(1L)).thenReturn(order);

            classicCaseService.markAsClassicCase(dto);

            ArgumentCaptor<OrderMainEntity> captor = ArgumentCaptor.forClass(OrderMainEntity.class);
            verify(orderMainMapper).updateById(captor.capture());
            OrderMainEntity updated = captor.getValue();

            assertEquals(StatusConstants.YES, updated.getIsClassicCase());
            assertEquals(100L, updated.getClassicCaseBy());
            assertEquals("优秀案例", updated.getClassicCaseRemark());
            assertNotNull(updated.getClassicCaseTime());

            verify(eventPublisher).publishEvent(any());
        }
    }

    @Test
    void markAsClassicCase_nullNeedsPhysicalDelivery_shouldUsePhaseCompletion() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
            dto.setOrderId(1L);
            OrderMainEntity order = new OrderMainEntity();
            order.setId(1L);
            order.setOrderCode("ORD001");
            order.setPhase(80);
            order.setNeedsPhysicalDelivery(null);
            order.setIsClassicCase(StatusConstants.NO);
            when(orderMainMapper.selectById(1L)).thenReturn(order);

            assertDoesNotThrow(() -> classicCaseService.markAsClassicCase(dto));
            verify(orderMainMapper).updateById(order);
        }
    }

    @Test
    void markAsClassicCase_orderNotFound() {
        MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
        dto.setOrderId(999L);
        dto.setRemark("测试");

        when(orderMainMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classicCaseService.markAsClassicCase(dto));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void markAsClassicCase_orderNotCompleted() {
        MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
        dto.setOrderId(1L);
        dto.setRemark("测试");

        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setPhase(60);
        order.setNeedsPhysicalDelivery(StatusConstants.NO);

        when(orderMainMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classicCaseService.markAsClassicCase(dto));
        assertEquals(ErrorCodeEnum.CLASSIC_CASE_ORDER_NOT_COMPLETED.getCode(), exception.getCode());
    }

    @Test
    void markAsClassicCase_alreadyMarked() {
        MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
        dto.setOrderId(1L);
        dto.setRemark("测试");

        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setPhase(80);
        order.setIsClassicCase(StatusConstants.YES);
        order.setNeedsPhysicalDelivery(StatusConstants.NO);

        when(orderMainMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classicCaseService.markAsClassicCase(dto));
        assertEquals(ErrorCodeEnum.CLASSIC_CASE_ALREADY_MARKED.getCode(), exception.getCode());
    }

    @Test
    void listClassicCases_withFilters() {
        ClassicCaseQueryDTO dto = new ClassicCaseQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        OrderMainEntity order1 = new OrderMainEntity();
        order1.setId(1L);
        order1.setOrderCode("ORD001");
        order1.setIsClassicCase(StatusConstants.YES);

        Page<OrderMainEntity> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(order1));
        page.setTotal(1);

        when(orderMainMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(orderQueryHelper.toOrderListVO(order1)).thenReturn(new com.yigongbao.module.order.vo.order.OrderListVO());
        when(userService.listByIds(anyList())).thenReturn(List.of());

        IPage<ClassicCaseVO> result = classicCaseService.listClassicCases(dto);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getClassicCaseDetail_success() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setOrderCode("ORD001");
        order.setIsClassicCase(StatusConstants.YES);

        when(orderMainMapper.selectById(1L)).thenReturn(order);
        com.yigongbao.module.order.vo.order.OrderDetailVO detail =
                new com.yigongbao.module.order.vo.order.OrderDetailVO();
        detail.setOrderCode("ORD001");
        when(orderMainService.buildOrderDetailWithoutPermissionCheck(eq(1L), eq(order)))
                .thenReturn(detail);

        ClassicCaseVO result = classicCaseService.getClassicCaseDetail(1L);

        assertNotNull(result);
        assertEquals("ORD001", result.getOrderCode());
    }

    @Test
    void getClassicCaseDetail_notClassicCase() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setIsClassicCase(StatusConstants.NO);

        when(orderMainMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classicCaseService.getClassicCaseDetail(1L));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void isClassicCase_true() {
        OrderMainEntity order = new OrderMainEntity();
        order.setIsClassicCase(StatusConstants.YES);

        when(orderMainMapper.selectById(1L)).thenReturn(order);

        assertTrue(classicCaseService.isClassicCase(1L));
    }

    @Test
    void isClassicCase_false() {
        OrderMainEntity order = new OrderMainEntity();
        order.setIsClassicCase(StatusConstants.NO);

        when(orderMainMapper.selectById(1L)).thenReturn(order);

        assertFalse(classicCaseService.isClassicCase(1L));
    }

    @Test
    void cancelClassicCaseMark_clearsProtectedMetadata() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setIsClassicCase(StatusConstants.YES);
        order.setClassicCaseBy(100L);
        order.setClassicCaseRemark("案例");
        order.setClassicCaseTime(LocalDateTime.now());
        when(orderMainMapper.selectById(1L)).thenReturn(order);

        classicCaseService.cancelClassicCaseMark(1L, "回滚");

        assertEquals(StatusConstants.NO, order.getIsClassicCase());
        assertNull(order.getClassicCaseBy());
        assertNull(order.getClassicCaseRemark());
        assertNull(order.getClassicCaseTime());
        verify(orderMainMapper).updateById(order);
    }

    @Test
    void cancelClassicCaseMark_isNoOpForMissingOrUnmarkedOrder() {
        when(orderMainMapper.selectById(2L)).thenReturn(null);
        classicCaseService.cancelClassicCaseMark(2L, "回滚");

        OrderMainEntity order = new OrderMainEntity();
        order.setId(3L);
        order.setIsClassicCase(StatusConstants.NO);
        when(orderMainMapper.selectById(3L)).thenReturn(order);
        classicCaseService.cancelClassicCaseMark(3L, "回滚");

        verify(orderMainMapper, never()).updateById(any(OrderMainEntity.class));
    }
}
