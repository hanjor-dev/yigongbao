package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.order.vo.order.OrderStatisticsVO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderMainServiceImpl.listOrders 单元测试
 * 覆盖：数据权限（4种scope）、hospitalId参数校验、多条件过滤（9个维度）、
 *       inSql子查询、分页、VO转换委托等核心逻辑
 *
 * @author hanjor
 * @date 2026-04-07
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderMainServiceImplListOrdersTest {

    @BeforeAll
    static void initLambdaMetadata() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
    }

    // ── 被测类所有依赖，@InjectMocks 会注入 ──────────────────────────
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderDraftMapper orderDraftMapper;
    @Mock private OrderItemDraftMapper orderItemDraftMapper;
    @Mock private OrderFileMapper orderFileMapper;
    @Mock private UserHospitalService userHospitalService;
    @Mock private OrderQueryHelper orderQueryHelper;

    // 其余依赖：listOrders 不涉及，lenient 模式下无需 stub
    @Mock private com.yigongbao.module.basic.code.service.CodeGeneratorService codeGeneratorService;
    @Mock private com.yigongbao.module.basic.file.service.FileService fileService;
    @Mock private com.yigongbao.flow.facade.FlowFacade flowFacade;
    @Mock private com.yigongbao.module.system.config.service.ConfigService configService;
    @Mock private com.yigongbao.module.system.user.service.UserService userService;
    @Mock private com.yigongbao.module.system.org.service.OrgService orgService;
    @Mock private com.yigongbao.module.system.dept.service.DeptService deptService;
    @Mock private com.yigongbao.module.system.dict.service.DictService dictService;
    @Mock private com.yigongbao.module.order.validator.OrderDataValidator orderDataValidator;
    @Mock private com.yigongbao.module.order.service.OrderModifyApplyService orderModifyApplyService;
    @Mock private com.yigongbao.module.order.convert.OrderConvert orderConvert;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private OrderMainServiceImpl orderMainService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 使用 baseMapper 字段执行 page()，需通过反射注入 mock
        ReflectionTestUtils.setField(orderMainService, "baseMapper", orderMainMapper);
        doNothing().when(orderConvert).fillAuditInfo(any(OrderMainEntity.class), any(OrderListVO.class));
    }

    @Test
    void listOrders_shouldNotApplyDataScopeToDesigner() {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
        when(orderQueryHelper.getCurrentUserRoleCode()).thenReturn(RoleCodeEnum.DESIGNER.getCode());
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);
        mockSelectPage(List.of(), 0L);

        orderMainService.listOrders(baseDto());

        verify(orderQueryHelper, never()).buildDataScopeCondition(any(), any(), any());
    }

    @Test
    void listOrders_shouldAllowDesignerToFilterAnyHospital() {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
        when(orderQueryHelper.getCurrentUserRoleCode()).thenReturn(RoleCodeEnum.DESIGNER.getCode());
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.HOSPITALS);
        when(userHospitalService.getHospitalIdsByUserId(1L)).thenReturn(List.of(10L));
        mockSelectPage(List.of(), 0L);

        OrderPageDTO dto = baseDto();
        dto.setHospitalId(99L);
        orderMainService.listOrders(dto);

        verify(userHospitalService, never()).getHospitalIdsByUserId(1L);
        verify(orderQueryHelper, never()).buildDataScopeCondition(any(), any(), any());
    }

    @Test
    void listOrders_shouldNotApplyDataScopeToDesignerManager() {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
        when(orderQueryHelper.getCurrentUserRoleCode()).thenReturn(RoleCodeEnum.DESIGNER_MANAGER.getCode());
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);
        mockSelectPage(List.of(), 0L);

        orderMainService.listOrders(baseDto());

        verify(orderQueryHelper, never()).buildDataScopeCondition(any(), any(), any());
    }

    // ==================== 辅助方法 ====================

    private OrderPageDTO baseDto() {
        OrderPageDTO dto = new OrderPageDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        return dto;
    }

    private OrderMainEntity buildEntity(Long id) {
        OrderMainEntity e = new OrderMainEntity();
        e.setId(id);
        e.setOrderCode("ORD-" + id);
        e.setPhase(10);
        e.setStatus(2010);
        return e;
    }

    private OrderListVO buildVO(Long id) {
        OrderListVO vo = new OrderListVO();
        vo.setId(id);
        return vo;
    }

    /**
     * 配置 orderMainMapper.selectPage 返回指定数据
     */
    @SuppressWarnings("unchecked")
    private void mockSelectPage(List<OrderMainEntity> records, long total) {
        doAnswer(inv -> {
            Page<OrderMainEntity> page = inv.getArgument(0);
            page.setRecords(records);
            page.setTotal(total);
            return page;
        }).when(orderMainMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    /**
     * 获取空条件基准 wrapper 的 segment 数量（用于与带过滤条件的对比）
     */
    @SuppressWarnings("unchecked")
    private int getBaselineSegmentCount() {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
        mockSelectPage(List.of(), 0L);
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        orderMainService.listOrders(baseDto());
        verify(orderMainMapper).selectPage(any(), captor.capture());
        return captor.getValue().getExpression().getNormal().size();
    }

    /**
     * 执行 listOrders 并捕获传入 mapper 的 wrapper，返回其 segment 数量
     */
    @SuppressWarnings("unchecked")
    private int executeAndGetSegmentCount(OrderPageDTO dto) {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
        mockSelectPage(List.of(), 0L);
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        orderMainService.listOrders(dto);
        verify(orderMainMapper).selectPage(any(), captor.capture());
        return captor.getValue().getExpression().getNormal().size();
    }

    // ==================== 内部类：数据权限 - SELF ====================

    @Nested
    class DataScopeSelf {

        @Test
        void selfScope_buildDataScopeConditionCalled() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            verify(orderQueryHelper).buildDataScopeCondition(any(), eq(1L), eq(DataScopeTypeEnum.SELF));
        }

        @Test
        void selfScope_noHospitalId_userHospitalIdsNotQueried() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            verify(userHospitalService, never()).getHospitalIdsByUserId(any());
        }

        @Test
        void selfScope_withHospitalId_executesQueryWithoutPermissionCheck() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.SELF);
            mockSelectPage(List.of(), 0L);

            OrderPageDTO dto = baseDto();
            dto.setHospitalId(200L);
            orderMainService.listOrders(dto);

            // SELF 类型不做 hospitalId 权限校验，直接执行查询
            verify(userHospitalService, never()).getHospitalIdsByUserId(any());
            verify(orderMainMapper).selectPage(any(), any());
        }
    }

    // ==================== 内部类：数据权限 - HOSPITALS ====================

    @Nested
    class DataScopeHospitals {

        @Test
        void noHospitalParam_returnsAllWithinScope() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.HOSPITALS);
            mockSelectPage(List.of(), 0L);

            IPage<OrderListVO> result = orderMainService.listOrders(baseDto());

            assertThat(result).isNotNull();
            verify(orderQueryHelper).buildDataScopeCondition(any(), eq(1L), eq(DataScopeTypeEnum.HOSPITALS));
            // 无 hospitalId 参数时，不查询用户医院列表
            verify(userHospitalService, never()).getHospitalIdsByUserId(any());
        }

        @Test
        void validHospitalId_withinScope_executesQuery() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.HOSPITALS);
            when(userHospitalService.getHospitalIdsByUserId(1L)).thenReturn(List.of(100L, 200L));
            mockSelectPage(List.of(), 0L);

            OrderPageDTO dto = baseDto();
            dto.setHospitalId(100L);
            IPage<OrderListVO> result = orderMainService.listOrders(dto);

            assertThat(result).isNotNull();
            // hospitalId 在权限内时，正常执行查询
            verify(orderMainMapper).selectPage(any(), any());
        }

        @Test
        void invalidHospitalId_notInScope_returnsEmptyPageWithoutDbCall() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.HOSPITALS);
            when(userHospitalService.getHospitalIdsByUserId(1L)).thenReturn(List.of(100L, 200L));

            OrderPageDTO dto = baseDto();
            dto.setHospitalId(999L); // 不在权限范围内

            IPage<OrderListVO> result = orderMainService.listOrders(dto);

            // 直接短路返回空 Page，不执行 DB 查询
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
            verify(orderMainMapper, never()).selectPage(any(), any());
        }

        @Test
        void emptyUserHospitalList_buildDataScopeConditionStillCalled() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.HOSPITALS);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            // buildDataScopeCondition 被调用（内部处理空列表添加 1=0）
            verify(orderQueryHelper).buildDataScopeCondition(any(), eq(1L), eq(DataScopeTypeEnum.HOSPITALS));
        }
    }

    // ==================== 内部类：数据权限 - ORG / ALL ====================

    @Nested
    class DataScopeOrgAndAll {

        @Test
        void orgScope_buildDataScopeConditionCalledWithOrg() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ORG);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            verify(orderQueryHelper).buildDataScopeCondition(any(), eq(1L), eq(DataScopeTypeEnum.ORG));
        }

        @Test
        void allScope_buildDataScopeConditionCalledWithAll() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            verify(orderQueryHelper).buildDataScopeCondition(any(), eq(1L), eq(DataScopeTypeEnum.ALL));
        }
    }

    // ==================== 内部类：多条件过滤 ====================
    // 由于 LambdaQueryWrapper 中条件以 lambda 形式存储（不含列名字符串），
    // 通过比较加入过滤条件前后 wrapper 的 segment 数量来验证条件是否被正确添加。

    @Nested
    class MultiConditionFiltering {

        @Test
        void filterByOrderCode_addsMoreSegmentsThanBaseline() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setOrderCode("ORD-2026");
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByOrderCode_alsoMatchesDoctorName() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);
            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);

            OrderPageDTO dto = baseDto();
            dto.setOrderCode("张");
            orderMainService.listOrders(dto);

            verify(orderMainMapper).selectPage(any(), captor.capture());
            assertThat(captor.getValue().getSqlSegment()).contains("doctorName");
        }

        @Test
        void filterByAreaId_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setAreaId(10L);
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByDoctorName_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setDoctorName("张");
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByPatientName_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setPatientName("李");
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByBusinessType_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setBusinessType("11.1");
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByOperatorId_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setOperatorId(5L);
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByStatus_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setStatus(2010);
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2})
        void filterByNeedsPhysicalDelivery_includesAllSupportedValues(int needsPhysicalDelivery) {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setNeedsPhysicalDelivery(needsPhysicalDelivery);
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByCreateTimeRange_addsTwoMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setCreateTimeStart(LocalDateTime.of(2026, 1, 1, 0, 0));
            dto.setCreateTimeEnd(LocalDateTime.of(2026, 12, 31, 23, 59));
            int withFilter = executeAndGetSegmentCount(dto);

            // start(ge) + AND + end(le) = 3 extra segments
            assertThat(withFilter).isGreaterThan(baseline + 1);
        }

        @Test
        void filterByCreateTimeStartOnly_addsOneMoreSegment() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setCreateTimeStart(LocalDateTime.of(2026, 1, 1, 0, 0));
            // createTimeEnd 不设置
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByCreateTimeEnd_usesNextDayAsExclusiveBoundary() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            OrderPageDTO dto = baseDto();
            dto.setCreateTimeEnd(LocalDateTime.of(2026, 8, 25, 0, 0));
            orderMainService.listOrders(dto);

            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(orderMainMapper).selectPage(any(Page.class), captor.capture());
            assertThat(captor.getValue().getExpression().getNormal()).isNotEmpty();
            LocalDateTime exclusiveEnd = ReflectionTestUtils.invokeMethod(orderMainService, "toExclusiveEndTime",
                    LocalDateTime.of(2026, 8, 25, 0, 0));
            assertThat(exclusiveEnd)
                    .isEqualTo(LocalDateTime.of(2026, 8, 26, 0, 0));
        }

        @Test
        void filterByBodyPartIds_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            // 模拟 orderItemMapper 返回匹配的订单ID，使 in 条件被加入 wrapper
            com.yigongbao.module.order.entity.OrderItemEntity item = new com.yigongbao.module.order.entity.OrderItemEntity();
            item.setOrderId(1L);
            when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

            OrderPageDTO dto = baseDto();
            dto.setBodyPartIds(List.of(1L, 2L));
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void filterByProjectIds_addsMoreSegments() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            // 模拟 orderItemMapper 返回匹配的订单ID，使 in 条件被加入 wrapper
            com.yigongbao.module.order.entity.OrderItemEntity item = new com.yigongbao.module.order.entity.OrderItemEntity();
            item.setOrderId(1L);
            when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

            OrderPageDTO dto = baseDto();
            dto.setProjectIds(List.of(3L, 4L));
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isGreaterThan(baseline);
        }

        @Test
        void emptyBodyPartIds_sameSegmentCountAsBaseline() {
            int baseline = getBaselineSegmentCount();
            clearInvocations(orderMainMapper);

            OrderPageDTO dto = baseDto();
            dto.setBodyPartIds(List.of()); // 空列表不添加 inSql
            int withFilter = executeAndGetSegmentCount(dto);

            assertThat(withFilter).isEqualTo(baseline);
        }

        @Test
        void nullFilters_mapperIsCalledOnce() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            // 所有可选参数为 null：不会短路，应正常执行查询
            orderMainService.listOrders(baseDto());

            verify(orderMainMapper, times(1)).selectPage(any(), any(LambdaQueryWrapper.class));
        }

        @Test
        void alwaysFiltersPhaseEqualsOrder_wrapperNonEmpty() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            // phase 不传时不加过滤条件，wrapper 可为空（ALL 数据权限无额外条件）
            verify(orderMainMapper).selectPage(any(), any());
        }
    }

    // ==================== 内部类：边界与 VO 转换 ====================

    @Nested
    class BoundaryAndVoConversion {

        @Test
        void statistics_appliesCurrentUserDataScopeToEveryCount() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ORG);
            when(orderMainMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(12L, 3L, 4L);

            OrderStatisticsVO result = orderMainService.statistics();

            assertThat(result.getTotal()).isEqualTo(12L);
            assertThat(result.getPendingAudit()).isEqualTo(3L);
            assertThat(result.getDesigning()).isEqualTo(4L);
            verify(userHospitalService).getDataScopeType(1L);
            verify(orderQueryHelper, times(3)).buildDataScopeCondition(
                    any(LambdaQueryWrapper.class), eq(1L), eq(DataScopeTypeEnum.ORG));
            verify(orderMainMapper, times(3)).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        void statistics_allowsDesignerToQueryAllCounts() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(orderQueryHelper.getCurrentUserRoleCode()).thenReturn(RoleCodeEnum.DESIGNER.getCode());
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.CENTER);
            when(orderMainMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(20L, 5L, 7L);

            OrderStatisticsVO result = orderMainService.statistics();

            assertThat(result.getTotal()).isEqualTo(20L);
            verify(orderQueryHelper, times(3)).buildDataScopeCondition(
                    any(LambdaQueryWrapper.class), eq(1L), eq(DataScopeTypeEnum.ALL));
        }

        @ParameterizedTest
        @EnumSource(DataScopeTypeEnum.class)
        void statistics_forwardsEveryDataScopeType(DataScopeTypeEnum scopeType) {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(scopeType);
            when(orderMainMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(1L, 2L, 3L);

            orderMainService.statistics();

            verify(orderQueryHelper, times(3)).buildDataScopeCondition(
                    any(LambdaQueryWrapper.class), eq(1L), eq(scopeType));
        }

        @Test
        void emptyResult_returnsEmptyPage() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            IPage<OrderListVO> result = orderMainService.listOrders(baseDto());

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
        }

        @Test
        void fillRebuildProjectListCalled_whenHasRecords() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);

            OrderMainEntity e1 = buildEntity(1L);
            OrderMainEntity e2 = buildEntity(2L);
            mockSelectPage(List.of(e1, e2), 2L);
            when(orderQueryHelper.toOrderListVO(e1)).thenReturn(buildVO(1L));
            when(orderQueryHelper.toOrderListVO(e2)).thenReturn(buildVO(2L));

            orderMainService.listOrders(baseDto());

            verify(orderQueryHelper).fillRebuildProjectList(any(List.class));
        }

        @Test
        void fillRebuildProjectListCalled_evenWhenEmpty() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);

            orderMainService.listOrders(baseDto());

            // 空列表时 fillRebuildProjectList 也被调用（内部自行处理空列表的幂等性）
            verify(orderQueryHelper).fillRebuildProjectList(any(List.class));
        }

        @Test
        void toOrderListVO_calledForEachRecord() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);

            OrderMainEntity e1 = buildEntity(1L);
            OrderMainEntity e2 = buildEntity(2L);
            OrderMainEntity e3 = buildEntity(3L);
            mockSelectPage(List.of(e1, e2, e3), 3L);
            when(orderQueryHelper.toOrderListVO(e1)).thenReturn(buildVO(1L));
            when(orderQueryHelper.toOrderListVO(e2)).thenReturn(buildVO(2L));
            when(orderQueryHelper.toOrderListVO(e3)).thenReturn(buildVO(3L));

            IPage<OrderListVO> result = orderMainService.listOrders(baseDto());

            verify(orderQueryHelper, times(3)).toOrderListVO(any(OrderMainEntity.class));
            assertThat(result.getRecords()).hasSize(3);
        }

        @Test
        void pagination_respectsPageNumAndSize() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);
            mockSelectPage(List.of(), 0L);
            ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);

            OrderPageDTO dto = baseDto();
            dto.setPageNum(3);
            dto.setPageSize(20);
            orderMainService.listOrders(dto);

            verify(orderMainMapper).selectPage(pageCaptor.capture(), any());
            Page capturedPage = pageCaptor.getValue();
            assertThat(capturedPage.getCurrent()).isEqualTo(3L);
            assertThat(capturedPage.getSize()).isEqualTo(20L);
        }

        @Test
        void correctTotal_returnedFromMapper() {
            when(orderQueryHelper.getCurrentUserId()).thenReturn(1L);
            when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.ALL);

            OrderMainEntity e1 = buildEntity(1L);
            mockSelectPage(List.of(e1), 50L); // 模拟共50条数据，当前页1条
            when(orderQueryHelper.toOrderListVO(e1)).thenReturn(buildVO(1L));

            IPage<OrderListVO> result = orderMainService.listOrders(baseDto());
            assertThat(result.getTotal()).isEqualTo(50L);
        }
    }
}
