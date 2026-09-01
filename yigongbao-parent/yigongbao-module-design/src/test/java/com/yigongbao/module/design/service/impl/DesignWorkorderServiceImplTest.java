package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.service.FlowStatusColorResolver;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
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
import com.yigongbao.module.design.enums.DesignModeEnum;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.design.vo.SubmitCheckVO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import com.yigongbao.module.order.service.OrderItemService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeAll
    static void initLambdaMetadata() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignProductEntity.class);
        TableInfoHelper.initTableInfo(assistant, OrderModificationApplyEntity.class);
    }

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
    @Mock private ConfigService configService;
    @Mock private ObjectMapper objectMapper;
    @Mock private FlowFacade flowFacade;
    @Mock private FlowStatusColorResolver flowStatusColorResolver;
    @Mock private OrderFileMapper orderFileMapper;
    @Mock private OrderCancelApplyService cancelApplyService;
    @Mock private OrderModificationApplyMapper orderModificationApplyMapper;
    @Mock private DesignFileService designFileService;
    @Mock private DesignDocService designDocService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DesignWorkorderServiceImpl service;

    @BeforeEach
    void setUpCommonMocks() {
        when(cancelApplyService.hasPendingCancelApply(anyLong())).thenReturn(false);
        when(orderFileMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(designFileService.listPackages(anyLong())).thenReturn(Collections.emptyList());
        when(designFileService.listModels(anyLong())).thenReturn(Collections.emptyList());
        when(designFileService.getReport(anyLong())).thenReturn(null);
        when(designDocService.getLatestInstructionMap(anySet())).thenReturn(Collections.emptyMap());
        when(designDocService.getLatestDrawingMap(anySet())).thenReturn(Collections.emptyMap());
    }

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
        @DisplayName("按状态值精确筛选，不扩展为后续状态")
        void listWorkorders_statusFilterMatchesExactValue() {
            DesignWorkorderQueryDTO dto = new DesignWorkorderQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());

            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(designQueryHelper.getCurrentUser()).thenReturn(new UserEntity());
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            when(orderMainService.page(any(), any())).thenReturn(new Page<>(1, 10, 0));

            ArgumentCaptor<LambdaQueryWrapper<OrderMainEntity>> wrapperCaptor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);

            service.listWorkorders(dto);

            verify(orderMainService).page(any(), wrapperCaptor.capture());
            assertThat(wrapperCaptor.getValue().getExpression().getNormal()).hasSize(3);
        }

        @Test
        @DisplayName("结束日期使用次日零点作为排他边界")
        void listWorkorders_createTimeEndUsesNextDayExclusiveBoundary() {
            DesignWorkorderQueryDTO dto = new DesignWorkorderQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setCreateTimeEnd(LocalDateTime.of(2026, 8, 25, 0, 0));

            when(designQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(designQueryHelper.getCurrentUser()).thenReturn(new UserEntity());
            when(userHospitalService.getDataScopeType(any())).thenReturn(DataScopeTypeEnum.ALL);
            when(orderMainService.page(any(), any())).thenReturn(new Page<>(1, 10, 0));

            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            service.listWorkorders(dto);

            verify(orderMainService).page(any(), captor.capture());
            assertThat(captor.getValue().getExpression().getNormal()).isNotEmpty();
            LocalDateTime exclusiveEnd = ReflectionTestUtils.invokeMethod(service, "toExclusiveEndTime",
                    LocalDateTime.of(2026, 8, 25, 0, 0));
            assertThat(exclusiveEnd)
                    .isEqualTo(LocalDateTime.of(2026, 8, 26, 0, 0));
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
        @DisplayName("无图纸时 latestDrawings 返回空数组")
        void getWorkorderDetail_noDrawing_returnsEmptyLatestDrawings() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            DesignPackageVO pkg = new DesignPackageVO();
            pkg.setId(1L);
            when(designFileService.listPackages(10L)).thenReturn(List.of(pkg));
            when(designDocService.getLatestDrawingGroups(anySet())).thenReturn(Collections.emptyMap());

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            assertNotNull(vo.getPackageList().get(0).getLatestDrawings());
            assertTrue(vo.getPackageList().get(0).getLatestDrawings().isEmpty());
        }

        @Test
        @DisplayName("多分类包只生成一张图纸时 latestDrawing 仍为空")
        void getWorkorderDetail_multiCategoryWithOneDrawing_doesNotFillLegacyField() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            DesignPackageVO pkg = new DesignPackageVO();
            pkg.setId(1L);
            when(designFileService.listPackages(10L)).thenReturn(List.of(pkg));
            DesignDocVersionVO drawing = new DesignDocVersionVO();
            drawing.setId(100L);
            drawing.setProductCategory("17.1");
            when(designDocService.getLatestDrawingGroups(anySet()))
                    .thenReturn(Map.of(1L, List.of(drawing)));
            DesignProductEntity model = new DesignProductEntity();
            model.setPackageId(1L);
            model.setProductCategory("17.1");
            DesignProductEntity guide = new DesignProductEntity();
            guide.setPackageId(1L);
            guide.setProductCategory("17.2");
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(model, guide));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            assertEquals(1, vo.getPackageList().get(0).getLatestDrawings().size());
            assertNull(vo.getPackageList().get(0).getLatestDrawing());
        }

        @Test
        @DisplayName("单分类包继续回填 latestDrawing")
        void getWorkorderDetail_singleCategory_fillsLegacyField() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            DesignPackageVO pkg = new DesignPackageVO();
            pkg.setId(1L);
            when(designFileService.listPackages(10L)).thenReturn(List.of(pkg));
            DesignDocVersionVO drawing = new DesignDocVersionVO();
            drawing.setId(100L);
            drawing.setProductCategory("17.1");
            when(designDocService.getLatestDrawingGroups(anySet()))
                    .thenReturn(Map.of(1L, List.of(drawing)));
            DesignProductEntity model = new DesignProductEntity();
            model.setPackageId(1L);
            model.setProductCategory("17.1");
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(model));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            assertSame(drawing, vo.getPackageList().get(0).getLatestDrawing());
        }

        @Test
        @DisplayName("历史无分类包继续回填 latestDrawing")
        void getWorkorderDetail_legacyNullCategory_fillsLegacyField() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            DesignPackageVO pkg = new DesignPackageVO();
            pkg.setId(1L);
            when(designFileService.listPackages(10L)).thenReturn(List.of(pkg));
            DesignDocVersionVO drawing = new DesignDocVersionVO();
            drawing.setId(100L);
            drawing.setProductCategory(null);
            when(designDocService.getLatestDrawingGroups(anySet()))
                    .thenReturn(Map.of(1L, List.of(drawing)));
            DesignProductEntity legacyProduct = new DesignProductEntity();
            legacyProduct.setPackageId(1L);
            legacyProduct.setProductCategory(null);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(legacyProduct));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);

            assertSame(drawing, vo.getPackageList().get(0).getLatestDrawing());
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
        @DisplayName("所有条件满足时 canSubmit=true（在线模式，无需修订版文件）")
        void getWorkorderDetail_submitCheck_canSubmit() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            // 在线模式下跳过修订版文件校验
            when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.ONLINE.getCode()));

            // 一个数据包
            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(1L);
            pkg.setOrderId(10L);
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));

            // 打印信息
            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(1L);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));

            // 指令单（在线模式下，指令单必须已确认）
            DesignInstructionEntity instruction = new DesignInstructionEntity();
            instruction.setPackageId(1L);
            instruction.setVersionSeq(1);
            instruction.setIsConfirmed(1);
            when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instruction));

            // 图纸（在线模式下，图纸必须已确认）
            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(1L);
            drawing.setVersionSeq(1);
            drawing.setIsConfirmed(1);
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
            assertTrue(check.getHasDrawingConfirmed());
            assertTrue(check.getHasInstructionConfirmed());
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

        @Test
        @DisplayName("在线模式：图纸未确认时 canSubmit=false，blockReason=请确认图纸")
        void getWorkorderDetail_submitCheck_onlineMode_drawingNotConfirmed() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.ONLINE.getCode()));

            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(1L);
            pkg.setOrderId(10L);
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));

            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(1L);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));

            // 指令单已确认
            DesignInstructionEntity instruction = new DesignInstructionEntity();
            instruction.setPackageId(1L);
            instruction.setVersionSeq(1);
            instruction.setIsConfirmed(1);
            when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instruction));

            // 图纸未确认
            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(1L);
            drawing.setVersionSeq(1);
            drawing.setIsConfirmed(0);
            when(designDrawingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(drawing));

            when(designModelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
            when(fileService.listByBiz(eq("10.5"), eq(10L))).thenReturn(List.of(new FileVO()));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);
            SubmitCheckVO check = vo.getSubmitCheck();

            assertFalse(check.getHasDrawingConfirmed());
            assertFalse(check.getCanSubmit());
            assertEquals("请确认图纸", check.getBlockReason());
        }

        @Test
        @DisplayName("在线模式：指令单未确认时 canSubmit=false，blockReason=请确认指令单")
        void getWorkorderDetail_submitCheck_onlineMode_instructionNotConfirmed() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.ONLINE.getCode()));

            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(1L);
            pkg.setOrderId(10L);
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));

            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(1L);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));

            // 指令单未确认
            DesignInstructionEntity instruction = new DesignInstructionEntity();
            instruction.setPackageId(1L);
            instruction.setVersionSeq(1);
            instruction.setIsConfirmed(0);
            when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instruction));

            // 图纸已确认
            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(1L);
            drawing.setVersionSeq(1);
            drawing.setIsConfirmed(1);
            when(designDrawingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(drawing));

            when(designModelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
            when(fileService.listByBiz(eq("10.5"), eq(10L))).thenReturn(List.of(new FileVO()));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);
            SubmitCheckVO check = vo.getSubmitCheck();

            assertFalse(check.getHasInstructionConfirmed());
            assertFalse(check.getCanSubmit());
            assertEquals("请确认指令单", check.getBlockReason());
        }

        @Test
        @DisplayName("离线模式：跳过图纸和指令单确认校验（hasDrawingConfirmed=true, hasInstructionConfirmed=true）")
        void getWorkorderDetail_submitCheck_offlineMode_ignoresDrawingConfirmed() {
            when(orderMainService.getById(10L)).thenReturn(buildOrder(10L));
            when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.OFFLINE.getCode()));

            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(1L);
            pkg.setOrderId(10L);
            when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));

            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(1L);
            when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));

            // 指令单有修订版，未确认（离线模式忽略）
            DesignInstructionEntity instrWithRevised = new DesignInstructionEntity();
            instrWithRevised.setPackageId(1L);
            instrWithRevised.setVersionSeq(1);
            instrWithRevised.setIsConfirmed(0);
            instrWithRevised.setRevisedFileId("revised-002");
            when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instrWithRevised));

            // 图纸有修订版，未确认（离线模式忽略）
            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(1L);
            drawing.setVersionSeq(1);
            drawing.setIsConfirmed(0);
            drawing.setRevisedFileId("revised-001");
            when(designDrawingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(drawing));

            when(designModelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
            when(fileService.listByBiz(eq("10.5"), eq(10L))).thenReturn(List.of(new FileVO()));

            DesignWorkorderDetailVO vo = service.getWorkorderDetail(10L);
            SubmitCheckVO check = vo.getSubmitCheck();

            // 当前提交校验统一检查 isConfirmed，设计模式只影响前端展示，不再跳过确认状态。
            assertFalse(check.getHasDrawingConfirmed());
            assertFalse(check.getHasInstructionConfirmed());
            assertFalse(check.getCanSubmit());
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
                when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.START_DESIGN), any(FlowOperator.class), eq(1)))
                        .thenReturn(TransitionResult.of(20, FlowStatusEnum.DESIGN_IN_PROGRESS.getValue()));
                when(orderMainService.update(any())).thenReturn(true);

                assertDoesNotThrow(() -> service.startDesign(1L, 1));

                // 验证订单字段已回写
                verify(orderMainService).update(any());
            }
        }

        @Test
        @DisplayName("订单不存在，抛 ORDER_NOT_FOUND")
        void startDesign_orderNotFound() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                when(orderMainService.getById(1L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> service.startDesign(1L, 1));
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
                        () -> service.startDesign(1L, 1));
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
                        () -> service.startDesign(1L, 1));
                assertEquals(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH.getCode(), ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("updateEvaluationOpinion")
    class UpdateEvaluationOpinion {

        @Test
        @DisplayName("订单存在时仅更新评估意见")
        void success_updatesOpinionOnly() {
            when(orderMainService.getById(1L)).thenReturn(buildOrder(1L));

            service.updateEvaluationOpinion(1L, "影像数据清晰，可以进行设计");

            verify(orderMainService).updateById(argThat(order ->
                    order.getId().equals(1L)
                            && "影像数据清晰，可以进行设计".equals(order.getDataEvaluationOpinion())
                            && order.getOrderCode() == null));
        }

        @Test
        @DisplayName("订单不存在时抛出 ORDER_NOT_FOUND")
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateEvaluationOpinion(999L, "评估意见"));

            assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
            verify(orderMainService, never()).updateById(any());
        }
    }


    @Nested
    @DisplayName("completeDesign")
    class CompleteDesign {

        @Test
        @DisplayName("成功完成设计 - 需要实体交付")
        void success_needsPhysicalDelivery() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setId(1L);
                order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
                order.setDesignerId(100L);
                order.setNeedsPhysicalDelivery(1);
                when(orderMainService.getById(1L)).thenReturn(order);

                DesignPackageEntity pkg = new DesignPackageEntity();
                pkg.setId(10L);
                pkg.setOrderId(1L);
                pkg.setIsDeleted(StatusConstants.NOT_DELETED);
                when(designPackageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pkg));
                DesignProductEntity product = new DesignProductEntity();
                product.setPackageId(10L);
                when(designProductMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));
                DesignInstructionEntity instruction = new DesignInstructionEntity();
                instruction.setPackageId(10L);
                instruction.setVersionSeq(1);
                instruction.setIsConfirmed(StatusConstants.CONFIRMED);
                when(designInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(instruction));
                DesignDrawingEntity drawing = new DesignDrawingEntity();
                drawing.setPackageId(10L);
                drawing.setVersionSeq(1);
                drawing.setIsConfirmed(StatusConstants.CONFIRMED);
                when(designDrawingMapper.selectList(any(Wrapper.class))).thenReturn(List.of(drawing));

                UserEntity user = new UserEntity();
                user.setId(100L);
                user.setRealName("设计师A");
                when(userService.getById(100L)).thenReturn(user);

                TransitionResult mockResult = TransitionResult.of(FlowStatusEnum.PENDING_PRINT.getValue(),
                    FlowStatusEnum.DESIGN_COMPLETED.getValue());
                when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.COMPLETE_DESIGN), any(), eq(1)))
                        .thenReturn(mockResult);
                when(orderMainService.update(any())).thenReturn(true);

                assertDoesNotThrow(() -> service.completeDesign(1L, 1));
                verify(flowFacade).executeFlow(eq(1L), eq(FlowActionEnum.COMPLETE_DESIGN), any(), eq(1));
                verify(orderMainService).update(any());
            }
        }

        @Test
        @DisplayName("成功完成设计 - 无需实体交付")
        void success_noPhysicalDelivery() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setId(1L);
                order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
                order.setDesignerId(100L);
                order.setNeedsPhysicalDelivery(0);
                when(orderMainService.getById(1L)).thenReturn(order);

                UserEntity user = new UserEntity();
                user.setId(100L);
                user.setRealName("设计师A");
                when(userService.getById(100L)).thenReturn(user);

                TransitionResult mockResult = TransitionResult.of(FlowStatusEnum.DESIGN_COMPLETED.getValue(),
                    FlowStatusEnum.DESIGN_COMPLETED.getValue());
                when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.COMPLETE_DESIGN), any(), eq(1)))
                        .thenReturn(mockResult);
                when(orderMainService.update(any())).thenReturn(true);

                assertDoesNotThrow(() -> service.completeDesign(1L, 1));
                verify(flowFacade).executeFlow(eq(1L), eq(FlowActionEnum.COMPLETE_DESIGN), any(), eq(1));
                verify(orderMainService).update(any());
            }
        }

        @Test
        @DisplayName("订单不存在，抛 ORDER_NOT_FOUND")
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);
            assertThrows(BusinessException.class, () -> service.completeDesign(999L, 1));
        }

        @Test
        @DisplayName("订单状态非 DESIGN_IN_PROGRESS，抛 ORDER_STATUS_ERROR")
        void wrongStatus() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setId(1L);
                order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue());
                order.setDesignerId(100L);
                when(orderMainService.getById(1L)).thenReturn(order);

                assertThrows(BusinessException.class, () -> service.completeDesign(1L, 1));
            }
        }

        @Test
        @DisplayName("非分配设计师，抛 ORDER_DESIGNER_MISMATCH")
        void notAssignedDesigner() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                OrderMainEntity order = new OrderMainEntity();
                order.setId(1L);
                order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
                order.setDesignerId(200L);
                when(orderMainService.getById(1L)).thenReturn(order);

                assertThrows(BusinessException.class, () -> service.completeDesign(1L, 1));
            }
        }
    }
}
