package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.design.vo.SubmitCheckVO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.service.OrderItemService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignWorkorderServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignWorkorderService 单元测试")
class DesignWorkorderServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private OrderItemService orderItemService;
    @Mock private FileService fileService;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignProductMapper designProductMapper;
    @Mock private DesignInstructionMapper designInstructionMapper;
    @Mock private DesignDrawingMapper designDrawingMapper;
    @Mock private DesignModelMapper designModelMapper;
    @Mock private DesignReviewMapper designReviewMapper;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private DesignQueryHelper designQueryHelper;
    @Mock private ObjectMapper objectMapper;
    @Mock private FlowFacade flowFacade;

    @InjectMocks
    private DesignWorkorderServiceImpl service;

    private OrderMainEntity buildOrder(Long id) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setOrderCode("ORD-" + id);
        order.setStatus(10);
        order.setPhase(20);
        order.setOrderType(1);
        order.setNeedsPhysicalDelivery(1);
        order.setPatientName("张三");
        order.setPatientGender("10.1");
        order.setHospitalId(100L);
        order.setHospitalName("测试医院");
        order.setDesignerId(1L);
        order.setDesignerName("设计师A");
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    @Nested
    @DisplayName("listWorkorders")
    class ListWorkorders {

        @Test
        @DisplayName("正常分页查询——返回 VO 列表")
        void listWorkorders_success() {
            // 准备入参
            DesignWorkorderQueryDTO dto = new DesignWorkorderQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);

            // Mock：当前用户信息和数据权限
            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            UserEntity user = new UserEntity();
            user.setId(1L);
            when(designQueryHelper.getCurrentUser()).thenReturn(user);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);

            // Mock：分页查询返回一条订单
            OrderMainEntity order = buildOrder(10L);
            Page<OrderMainEntity> page = new Page<>(1, 10, 1);
            page.setRecords(List.of(order));
            when(orderMainService.page(any(), any())).thenReturn(page);

            // Mock：批量填充子查询
            when(orderItemService.listByOrderIds(any())).thenReturn(Collections.emptyList());
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
            when(designReviewMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

            // 执行
            IPage<DesignWorkorderListVO> result = service.listWorkorders(dto);

            // 断言
            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals(10L, result.getRecords().get(0).getId());
        }

        @Test
        @DisplayName("pageSize 超过 100 时自动截断为 100")
        void listWorkorders_pageSizeCapped() {
            DesignWorkorderQueryDTO dto = new DesignWorkorderQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(200);

            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(designQueryHelper.getCurrentUser()).thenReturn(new UserEntity());
            when(userHospitalService.getDataScopeType(any())).thenReturn(DataScopeTypeEnum.ALL);

            Page<OrderMainEntity> emptyPage = new Page<>(1, 100, 0);
            emptyPage.setRecords(Collections.emptyList());
            when(orderMainService.page(any(), any())).thenReturn(emptyPage);

            IPage<DesignWorkorderListVO> result = service.listWorkorders(dto);

            // 验证传入 page 方法的 Page 对象 size=100
            verify(orderMainService).page(argThat(p -> ((Page<?>) p).getSize() == 100), any());
        }

        @Test
        @DisplayName("包含重建项目摘要时正确拼接")
        void listWorkorders_rebuildProjectSummaryFilled() {
            DesignWorkorderQueryDTO dto = new DesignWorkorderQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);

            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(designQueryHelper.getCurrentUser()).thenReturn(new UserEntity());
            when(userHospitalService.getDataScopeType(any())).thenReturn(DataScopeTypeEnum.ALL);

            OrderMainEntity order = buildOrder(10L);
            Page<OrderMainEntity> page = new Page<>(1, 10, 1);
            page.setRecords(List.of(order));
            when(orderMainService.page(any(), any())).thenReturn(page);

            // 构造 OrderItem
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(10L);
            item.setBodyPartName("左髋骨");
            item.setProjectName("导板");
            when(orderItemService.listByOrderIds(any())).thenReturn(List.of(item));
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
            when(designReviewMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

            IPage<DesignWorkorderListVO> result = service.listWorkorders(dto);

            assertEquals("左髋骨导板", result.getRecords().get(0).getRebuildProjectSummary());
        }
    }

    @Nested
    @DisplayName("getWorkorderDetail")
    class GetWorkorderDetail {

        @BeforeEach
        void setUp() {
            // 通用子查询 mock
            when(designReviewMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
            when(orderItemService.listByOrderId(any())).thenReturn(Collections.emptyList());
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
            when(designModelMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
            when(fileService.listByBiz(any(), any())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("订单存在——返回详情 VO")
        void getWorkorderDetail_success() {
            OrderMainEntity order = buildOrder(10L);
            when(orderMainService.getById(10L)).thenReturn(order);

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            assertNotNull(vo);
            assertEquals(10L, vo.getId());
            assertEquals("ORD-10", vo.getOrderCode());
        }

        @Test
        @DisplayName("订单不存在——抛出 DATA_NOT_FOUND")
        void getWorkorderDetail_notFound() {
            when(orderMainService.getById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getWorkorderDetail(999L));
            assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无数据包时 submitCheck.hasPackage=false 且 canSubmit=false")
        void getWorkorderDetail_submitCheck_noPackage() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            SubmitCheckVO check = vo.getSubmitCheck();
            assertFalse(check.getHasPackage());
            assertFalse(check.getCanSubmit());
            assertEquals("请先上传打印文件数据包", check.getBlockReason());
        }

        @Test
        @DisplayName("所有条件满足时 canSubmit=true")
        void getWorkorderDetail_submitCheck_canSubmit() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));

            // 一个数据包
            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(1L);
            pkg.setOrderId(10L);
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));

            // 打印信息
            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(1L);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));

            // 指令单
            DesignInstructionEntity instruction = new DesignInstructionEntity();
            instruction.setPackageId(1L);
            when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instruction));

            // 图纸
            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(1L);
            when(designDrawingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(drawing));

            // 可视化模型
            when(designModelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

            // 设计报告
            when(fileService.listByBiz(eq("10.5"), eq(10L))).thenReturn(List.of(new FileVO()));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);
            SubmitCheckVO check = vo.getSubmitCheck();

            assertTrue(check.getHasPackage());
            assertTrue(check.getHasPrintInfo());
            assertTrue(check.getHasInstruction());
            assertTrue(check.getHasDrawing());
            assertTrue(check.getHasModel());
            assertTrue(check.getHasReport());
            assertTrue(check.getCanSubmit());
            assertNull(check.getBlockReason());
        }

        @Test
        @DisplayName("有驳回记录时填充 rejectReason")
        void getWorkorderDetail_rejectReasonFilled() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));

            DesignReviewEntity review = new DesignReviewEntity();
            review.setOrderId(10L);
            review.setReviewResult(StatusConstants.NO);
            review.setRejectReason("设计文件不符合规格");
            review.setCreateTime(LocalDateTime.now());
            when(designReviewMapper.selectList(any(Wrapper.class))).thenReturn(List.of(review));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);
            assertEquals("设计文件不符合规格", vo.getRejectReason());
        }
    }

    @Nested
    @DisplayName("getColumnConfig")
    class GetColumnConfig {

        @Test
        @DisplayName("委托给 DesignQueryHelper")
        void getColumnConfig_delegatesToHelper() {
            DesignColumnConfigVO configVO = new DesignColumnConfigVO();
            when(designQueryHelper.getColumnConfig()).thenReturn(configVO);

            DesignColumnConfigVO result = service.getColumnConfig();
            assertSame(configVO, result);
        }
    }

    @Nested
    @DisplayName("saveColumnConfig")
    class SaveColumnConfig {

        @Test
        @DisplayName("正常保存——序列化并写入用户实体")
        void saveColumnConfig_success() throws Exception {
            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            UserEntity user = new UserEntity();
            user.setId(1L);
            when(userService.getById(1L)).thenReturn(user);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"module\":\"design\",\"columns\":[]}");

            SaveDesignColumnConfigDTO dto = new SaveDesignColumnConfigDTO();
            dto.setColumns(Collections.emptyList());

            service.saveColumnConfig(dto);

            verify(userService).updateById(argThat(u -> u.getId().equals(1L)));
        }

        @Test
        @DisplayName("用户不存在——抛出 DATA_NOT_FOUND")
        void saveColumnConfig_userNotFound() {
            when(designQueryHelper.getCurrentUserId()).thenReturn(99L);
            when(userService.getById(99L)).thenReturn(null);

            SaveDesignColumnConfigDTO dto = new SaveDesignColumnConfigDTO();
            dto.setColumns(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.saveColumnConfig(dto));
            assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("JSON 序列化失败——抛出 500 业务异常")
        void saveColumnConfig_jsonError() throws Exception {
            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            UserEntity user = new UserEntity();
            user.setId(1L);
            when(userService.getById(1L)).thenReturn(user);
            when(objectMapper.writeValueAsString(any())).thenThrow(
                    new com.fasterxml.jackson.core.JsonProcessingException("error") {});

            SaveDesignColumnConfigDTO dto = new SaveDesignColumnConfigDTO();
            dto.setColumns(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.saveColumnConfig(dto));
            assertEquals(500, ex.getCode());
        }
    }

    @Nested
    @DisplayName("startDesign")
    class StartDesign {

        @Test
        @DisplayName("成功开始设计")
        void startDesign_success() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setId(1L);
                order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue());
                order.setDesignerId(100L);

                UserEntity user = new UserEntity();
                user.setId(100L);
                user.setRealName("张设计");

                when(orderMainService.getById(1L)).thenReturn(order);
                when(userService.getById(100L)).thenReturn(user);
                when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.START_DESIGN), any(FlowOperator.class)))
                        .thenReturn(TransitionResult.of(20, FlowStatusEnum.DESIGN_IN_PROGRESS.getValue()));

                assertDoesNotThrow(() -> service.startDesign(1L));

                // 验证订单字段已回写
                verify(orderMainService).updateById(argThat((OrderMainEntity o) ->
                        o.getStatus().equals(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue())
                                && o.getDesignStartTime() != null
                                && Long.valueOf(100L).equals(o.getCurrentHandlerId())
                                && "张设计".equals(o.getCurrentHandlerName())));
            }
        }

        @Test
        @DisplayName("订单不存在，抛 ORDER_NOT_FOUND")
        void startDesign_orderNotFound() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                when(orderMainService.getById(1L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> service.startDesign(1L));
                assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("订单状态非 PENDING_DESIGN，抛 ORDER_STATUS_ERROR")
        void startDesign_wrongStatus() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
                order.setDesignerId(100L);
                when(orderMainService.getById(1L)).thenReturn(order);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> service.startDesign(1L));
                assertEquals(ErrorCodeEnum.ORDER_STATUS_ERROR.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("非分配设计师，抛 ORDER_DESIGNER_MISMATCH")
        void startDesign_notAssignedDesigner() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L);

                OrderMainEntity order = new OrderMainEntity();
                order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue());
                order.setDesignerId(100L);
                when(orderMainService.getById(1L)).thenReturn(order);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> service.startDesign(1L));
                assertEquals(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH.getCode(), ex.getCode());
            }
        }
    }
}
