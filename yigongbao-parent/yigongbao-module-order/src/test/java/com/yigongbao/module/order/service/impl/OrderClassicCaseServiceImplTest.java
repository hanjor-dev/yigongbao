package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constants.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.mapper.OrderMainMapper;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IClassicCaseFileService;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;

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

    @InjectMocks
    private OrderClassicCaseServiceImpl classicCaseService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(classicCaseService, orderMainMapper);
    }

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

            when(orderMainMapper.selectById(1L)).thenReturn(order);
            when(orderMainMapper.updateById(any())).thenReturn(1);

            classicCaseService.markAsClassicCase(dto);

            ArgumentCaptor<OrderMainEntity> captor = ArgumentCaptor.forClass(OrderMainEntity.class);
            verify(orderMainMapper).updateById(captor.capture());
            OrderMainEntity updated = captor.getValue();

            assertEquals(StatusConstants.YES, updated.getIsClassicCase());
            assertEquals(100L, updated.getClassicCaseBy());
            assertEquals("优秀案例", updated.getClassicCaseRemark());
            assertNotNull(updated.getClassicCaseTime());

            verify(classicCaseFileService).migrateFilesToClassicCase(1L, "ORD001");
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
        dto.setOrderCode("ORD");
        dto.setPatientName("张");

        OrderMainEntity order1 = new OrderMainEntity();
        order1.setId(1L);
        order1.setOrderCode("ORD001");
        order1.setIsClassicCase(StatusConstants.YES);

        Page<OrderMainEntity> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(order1));
        page.setTotal(1);

        when(orderMainMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

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
}
