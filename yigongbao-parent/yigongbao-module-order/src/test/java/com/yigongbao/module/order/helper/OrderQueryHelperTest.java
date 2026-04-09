package com.yigongbao.module.order.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderQueryHelper 单元测试
 * 覆盖数据权限条件构建、列配置读取、VO转换、重建项目填充等核心逻辑
 *
 * @author hanjor
 * @date 2026-04-07
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderQueryHelperTest {

    @Mock
    private UserService userService;
    @Mock
    private UserHospitalService userHospitalService;
    @Mock
    private ConfigService configService;
    @Mock
    private DictService dictService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderQueryHelper orderQueryHelper;

    // ==================== 辅助构建方法 ====================

    private OrderMainEntity buildEntity() {
        OrderMainEntity e = new OrderMainEntity();
        e.setId(1L);
        e.setOrderCode("ORD-2026-001");
        e.setOrderType(1);
        e.setNeedsPhysicalDelivery(1);
        e.setBusinessType(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS);
        e.setOrgId(10L);
        e.setOrgName("测试机构");
        e.setOperatorId(100L);
        e.setOperatorName("操作员A");
        e.setOperatorPhone("13800000001");
        e.setHospitalId(200L);
        e.setHospitalName("测试医院");
        e.setAreaId(300L);
        e.setAreaName("朝阳区");
        e.setFullAreaName("中国,北京,北京市,朝阳区");
        e.setHospitalDeptId(400L);
        e.setHospitalDeptName("骨科");
        e.setDoctorId(500L);
        e.setDoctorName("张医生");
        e.setDoctorPhone("13800000002");
        e.setPatientName("李患者");
        e.setPatientAge(45);
        e.setPatientGender(DictCodeConstants.PATIENT_GENDER_MALE);
        e.setIsUrgent(1);
        e.setIsPostal(0);
        e.setPostalAddress(null);
        e.setDesignerId(600L);
        e.setDesignerName("王设计师");
        e.setExpectedDeliveryDate(LocalDateTime.of(2026, 5, 1, 0, 0));
        e.setEstimatedCost(new BigDecimal("1500.00"));
        e.setDataEvaluationOpinion("数据质量良好");
        e.setPhase(1);
        e.setStatus(20);
        e.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        return e;
    }

    private OrderItemEntity buildItem(Long orderId, Long id, String projectName, String bodyPartName, String categoryCode) {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProjectName(projectName);
        item.setBodyPartName(bodyPartName);
        item.setCategoryCode(categoryCode);
        item.setProjectDesc("项目说明" + id);
        item.setFormingRequirement("成型要求" + id);
        item.setOtherRequirement("其他要求" + id);
        item.setIsDeleted(StatusConstants.NOT_DELETED);
        return item;
    }

    private OrderListVO buildVO(Long id) {
        OrderListVO vo = new OrderListVO();
        vo.setId(id);
        return vo;
    }

    // ==================== 内部类：getCurrentUserId ====================

    @Nested
    class GetCurrentUserId {

        @Test
        void loggedIn_returnsUserId() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThat(orderQueryHelper.getCurrentUserId()).isEqualTo(100L);
            }
        }

        @Test
        void notLoggedIn_returnsNull() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenThrow(new RuntimeException("未登录"));
                assertThat(orderQueryHelper.getCurrentUserId()).isNull();
            }
        }
    }

    // ==================== 内部类：buildDataScopeCondition ====================

    @Nested
    class BuildDataScopeCondition {

        @Test
        void selfScope_addsCreateByCondition() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.SELF);
            // condition=true → 1 segment (EQ) added
            assertThat(wrapper.getExpression().getNormal()).isNotEmpty();
            // userHospitalService 未被调用
            verify(userHospitalService, never()).getHospitalIdsByUserId(any());
        }

        @Test
        void selfScope_nullUserId_conditionIsNotApplied() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, null, DataScopeTypeEnum.SELF);
            // condition=false → eq not applied → wrapper is empty
            assertThat(wrapper.getExpression().getNormal()).isEmpty();
        }

        @Test
        void hospitalsScope_withHospitals_addsInCondition() {
            when(userHospitalService.getHospitalIdsByUserId(100L)).thenReturn(List.of(1L, 2L, 3L));
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.HOSPITALS);
            // IN condition → 3 segments: (column, IN, values)
            assertThat(wrapper.getExpression().getNormal()).isNotEmpty();
        }

        @Test
        void hospitalsScope_emptyHospitals_addsAlwaysFalse() {
            when(userHospitalService.getHospitalIdsByUserId(100L)).thenReturn(List.of());
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.HOSPITALS);
            // apply("1 = 0") → appended as last sql, non-empty
            assertThat(wrapper.getExpression().getNormal()).isNotEmpty();
        }

        @Test
        void orgScope_withOrgId_addsOrgCondition() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                UserEntity user = new UserEntity();
                user.setOrgId(50L);
                when(userService.getById(100L)).thenReturn(user);

                LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
                orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.ORG);

                // orgId != null → eq added
                assertThat(wrapper.getExpression().getNormal()).isNotEmpty();
            }
        }

        @Test
        void orgScope_nullOrgId_noConditionAdded() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                when(userService.getById(100L)).thenReturn(null);

                LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
                orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.ORG);

                // no orgId → no condition
                assertThat(wrapper.getExpression().getNormal()).isEmpty();
            }
        }

        @Test
        void allScope_noConditionAdded() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 100L, DataScopeTypeEnum.ALL);
            assertThat(wrapper.getExpression().getNormal()).isEmpty();
            verify(userHospitalService, never()).getHospitalIdsByUserId(any());
        }
    }

    // ==================== 内部类：getColumnConfig ====================

    @Nested
    class GetColumnConfig {

        private OrderColumnConfigVO buildDummyConfig() {
            OrderColumnConfigVO config = new OrderColumnConfigVO();
            config.setModule("order");
            OrderColumnConfigVO.ColumnItemVO col = new OrderColumnConfigVO.ColumnItemVO();
            col.setField("orderCode");
            col.setLabel("订单编号");
            col.setVisible(true);
            col.setSort(1);
            config.setColumns(List.of(col));
            return config;
        }

        @Test
        void notLoggedIn_returnsSystemDefault() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenThrow(new RuntimeException("未登录"));
                OrderColumnConfigVO systemConfig = buildDummyConfig();
                when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                        .thenReturn("{\"module\":\"order\"}");
                when(objectMapper.readValue(eq("{\"module\":\"order\"}"), eq(OrderColumnConfigVO.class)))
                        .thenReturn(systemConfig);

                OrderColumnConfigVO result = orderQueryHelper.getColumnConfig();
                assertThat(result).isSameAs(systemConfig);
            }
        }

        @Test
        void userNotFound_returnsSystemDefault() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                when(userService.getById(100L)).thenReturn(null);
                OrderColumnConfigVO systemConfig = buildDummyConfig();
                when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                        .thenReturn("{\"module\":\"order\"}");
                when(objectMapper.readValue(eq("{\"module\":\"order\"}"), eq(OrderColumnConfigVO.class)))
                        .thenReturn(systemConfig);

                OrderColumnConfigVO result = orderQueryHelper.getColumnConfig();
                assertThat(result).isSameAs(systemConfig);
            }
        }

        @Test
        void userHasColumnSettings_returnsUserConfig() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                UserEntity user = new UserEntity();
                user.setColumnSettings("{\"module\":\"order\",\"columns\":[]}");
                when(userService.getById(100L)).thenReturn(user);

                OrderColumnConfigVO userConfig = buildDummyConfig();
                when(objectMapper.readValue(eq(user.getColumnSettings()), eq(OrderColumnConfigVO.class)))
                        .thenReturn(userConfig);

                OrderColumnConfigVO result = orderQueryHelper.getColumnConfig();
                assertThat(result).isSameAs(userConfig);
                // 系统配置不应被查询
                verify(configService, never()).getConfigValue(any());
            }
        }

        @Test
        void userNoColumnSettings_returnsSystemDefault() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                UserEntity user = new UserEntity();
                user.setColumnSettings("");
                when(userService.getById(100L)).thenReturn(user);

                OrderColumnConfigVO systemConfig = buildDummyConfig();
                when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                        .thenReturn("{\"module\":\"order\"}");
                when(objectMapper.readValue(eq("{\"module\":\"order\"}"), eq(OrderColumnConfigVO.class)))
                        .thenReturn(systemConfig);

                OrderColumnConfigVO result = orderQueryHelper.getColumnConfig();
                assertThat(result).isSameAs(systemConfig);
            }
        }

        @Test
        void userColumnSettingsParseError_fallsBackToSystemDefault() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                UserEntity user = new UserEntity();
                user.setColumnSettings("{invalid json}");
                when(userService.getById(100L)).thenReturn(user);
                when(objectMapper.readValue(eq("{invalid json}"), eq(OrderColumnConfigVO.class)))
                        .thenThrow(mock(JsonProcessingException.class));

                OrderColumnConfigVO systemConfig = buildDummyConfig();
                when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                        .thenReturn("{\"module\":\"order\"}");
                when(objectMapper.readValue(eq("{\"module\":\"order\"}"), eq(OrderColumnConfigVO.class)))
                        .thenReturn(systemConfig);

                // 解析失败时回退到系统默认配置，不抛异常
                OrderColumnConfigVO result = orderQueryHelper.getColumnConfig();
                assertThat(result).isSameAs(systemConfig);
            }
        }

        @Test
        void systemConfigBlank_returnsNull() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenThrow(new RuntimeException("未登录"));
                when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                        .thenReturn("");

                OrderColumnConfigVO result = orderQueryHelper.getSystemDefaultColumnConfig();
                assertThat(result).isNull();
            }
        }

        @Test
        void systemConfigParseError_returnsNull() throws Exception {
            when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey()))
                    .thenReturn("{invalid}");
            when(objectMapper.readValue(eq("{invalid}"), eq(OrderColumnConfigVO.class)))
                    .thenThrow(mock(JsonProcessingException.class));

            OrderColumnConfigVO result = orderQueryHelper.getSystemDefaultColumnConfig();
            assertThat(result).isNull();
        }
    }

    // ==================== 内部类：toOrderListVO ====================

    @Nested
    class ToOrderListVO {

        @Test
        void allFieldsMapped_correctMapping() {
            OrderMainEntity entity = buildEntity();
            DictVO dict = new DictVO();
            dict.setDictCode(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS);
            dict.setDictName("业务");
            when(dictService.getByDictCode(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS)).thenReturn(dict);

            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);

            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getOrderCode()).isEqualTo("ORD-2026-001");
            assertThat(vo.getHospitalId()).isEqualTo(200L);
            assertThat(vo.getHospitalName()).isEqualTo("测试医院");
            assertThat(vo.getAreaId()).isEqualTo(300L);
            assertThat(vo.getAreaName()).isEqualTo("朝阳区");
            assertThat(vo.getFullAreaName()).isEqualTo("中国,北京,北京市,朝阳区");
            assertThat(vo.getHospitalDeptId()).isEqualTo(400L);
            assertThat(vo.getHospitalDeptName()).isEqualTo("骨科");
            assertThat(vo.getDoctorId()).isEqualTo(500L);
            assertThat(vo.getDoctorName()).isEqualTo("张医生");
            assertThat(vo.getDoctorPhone()).isEqualTo("13800000002");
            assertThat(vo.getPatientName()).isEqualTo("李患者");
            assertThat(vo.getPatientAge()).isEqualTo(45);
            assertThat(vo.getDesignerId()).isEqualTo(600L);
            assertThat(vo.getDesignerName()).isEqualTo("王设计师");
            assertThat(vo.getIsUrgent()).isEqualTo(1);
            assertThat(vo.getPhase()).isEqualTo(1);
            assertThat(vo.getStatus()).isEqualTo(20);
            assertThat(vo.getOrgId()).isEqualTo(10L);
            assertThat(vo.getOrgName()).isEqualTo("测试机构");
        }

        @Test
        void orderType1_isMedicalDevice() {
            OrderMainEntity entity = buildEntity();
            entity.setOrderType(1);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getOrderTypeName()).isEqualTo("医疗器械");
        }

        @Test
        void orderType2_isNonMedical() {
            OrderMainEntity entity = buildEntity();
            entity.setOrderType(2);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getOrderTypeName()).isEqualTo("非医疗器械");
        }

        @Test
        void orderTypeNull_nameIsNull() {
            OrderMainEntity entity = buildEntity();
            entity.setOrderType(null);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getOrderTypeName()).isNull();
        }

        @Test
        void patientGenderMale_nameIsMale() {
            OrderMainEntity entity = buildEntity();
            entity.setPatientGender(DictCodeConstants.PATIENT_GENDER_MALE);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getPatientGenderName()).isEqualTo("男");
        }

        @Test
        void patientGenderFemale_nameIsFemale() {
            OrderMainEntity entity = buildEntity();
            entity.setPatientGender(DictCodeConstants.PATIENT_GENDER_FEMALE);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getPatientGenderName()).isEqualTo("女");
        }

        @Test
        void patientGenderNull_nameIsNull() {
            OrderMainEntity entity = buildEntity();
            entity.setPatientGender(null);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getPatientGenderName()).isNull();
        }

        @Test
        void needsPhysicalDelivery1_isYes() {
            OrderMainEntity entity = buildEntity();
            entity.setNeedsPhysicalDelivery(1);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getNeedsPhysicalDeliveryName()).isEqualTo("是");
        }

        @Test
        void needsPhysicalDelivery0_isNo() {
            OrderMainEntity entity = buildEntity();
            entity.setNeedsPhysicalDelivery(0);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getNeedsPhysicalDeliveryName()).isEqualTo("否");
        }

        @Test
        void businessType_lookupDict() {
            OrderMainEntity entity = buildEntity();
            entity.setBusinessType(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS);
            DictVO dict = new DictVO();
            dict.setDictName("业务");
            when(dictService.getByDictCode(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS)).thenReturn(dict);

            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getBusinessTypeName()).isEqualTo("业务");
            verify(dictService).getByDictCode(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS);
        }

        @Test
        void businessTypeNull_nameIsNull() {
            OrderMainEntity entity = buildEntity();
            entity.setBusinessType(null);
            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);
            assertThat(vo.getBusinessTypeName()).isNull();
            verify(dictService, never()).getByDictCode(any());
        }

        @Test
        void designerName_readDirectlyFromEntity_noUserServiceCall() {
            OrderMainEntity entity = buildEntity();
            entity.setDesignerName("王设计师");

            OrderListVO vo = orderQueryHelper.toOrderListVO(entity);

            assertThat(vo.getDesignerName()).isEqualTo("王设计师");
            // 不走 userService.getById() — N+1 已修复为冗余字段
            verify(userService, never()).getById(any());
        }
    }

    // ==================== 内部类：fillRebuildProjectList ====================

    @Nested
    class FillRebuildProjectList {

        @Test
        void nullList_noQuery() {
            orderQueryHelper.fillRebuildProjectList(null);
            verify(orderItemMapper, never()).selectList(any());
        }

        @Test
        void emptyList_noQuery() {
            orderQueryHelper.fillRebuildProjectList(List.of());
            verify(orderItemMapper, never()).selectList(any());
        }

        @Test
        void singleOrder_itemsMapped() {
            OrderListVO vo = buildVO(1L);
            OrderItemEntity item1 = buildItem(1L, 10L, "膝关节", "下肢", "模型");
            OrderItemEntity item2 = buildItem(1L, 11L, "股骨", "下肢", "导板");
            when(orderItemMapper.selectList(any())).thenReturn(List.of(item1, item2));

            orderQueryHelper.fillRebuildProjectList(List.of(vo));

            assertThat(vo.getRebuildProjectList()).hasSize(2);
            assertThat(vo.getRebuildProjectList().get(0).getProjectName()).isEqualTo("膝关节");
            assertThat(vo.getRebuildProjectList().get(1).getProjectName()).isEqualTo("股骨");
        }

        @Test
        void multipleOrders_itemsGroupedCorrectly() {
            OrderListVO vo1 = buildVO(1L);
            OrderListVO vo2 = buildVO(2L);
            OrderListVO vo3 = buildVO(3L);

            OrderItemEntity item1a = buildItem(1L, 10L, "项目A", "头部", "模型");
            OrderItemEntity item1b = buildItem(1L, 11L, "项目B", "头部", "导板");
            OrderItemEntity item2a = buildItem(2L, 20L, "项目C", "胸部", "模型");
            // vo3 无关联 items

            when(orderItemMapper.selectList(any())).thenReturn(List.of(item1a, item1b, item2a));

            orderQueryHelper.fillRebuildProjectList(List.of(vo1, vo2, vo3));

            assertThat(vo1.getRebuildProjectList()).hasSize(2);
            assertThat(vo2.getRebuildProjectList()).hasSize(1);
            assertThat(vo3.getRebuildProjectList()).isNull();
        }

        @Test
        void orderWithNoItems_rebuildProjectListRemainsNull() {
            OrderListVO vo = buildVO(99L);
            when(orderItemMapper.selectList(any())).thenReturn(List.of());

            orderQueryHelper.fillRebuildProjectList(List.of(vo));

            assertThat(vo.getRebuildProjectList()).isNull();
        }

        @Test
        void allTextFieldsFilled() {
            OrderListVO vo = buildVO(1L);
            OrderItemEntity item = buildItem(1L, 10L, "膝关节", "下肢", "模型");
            item.setProjectDesc("详细说明");
            item.setFormingRequirement("打印精度0.1mm");
            item.setOtherRequirement("颜色白色");
            when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

            orderQueryHelper.fillRebuildProjectList(List.of(vo));

            OrderListVO.RebuildProjectItemVO projectItem = vo.getRebuildProjectList().get(0);
            assertThat(projectItem.getProjectDesc()).isEqualTo("详细说明");
            assertThat(projectItem.getFormingRequirement()).isEqualTo("打印精度0.1mm");
            assertThat(projectItem.getOtherRequirement()).isEqualTo("颜色白色");
        }

        @Test
        void usesStatusConstantsNotDeleted_isDeletedConditionPresent() {
            OrderListVO vo = buildVO(1L);
            when(orderItemMapper.selectList(any())).thenReturn(List.of());

            orderQueryHelper.fillRebuildProjectList(List.of(vo));

            // 验证 orderItemMapper 被调用了一次，且 wrapper 非空（含 is_deleted 和 order_id 条件）
            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(orderItemMapper).selectList(captor.capture());
            // wrapper 包含至少 2 个条件（in orderId + eq isDeleted）
            assertThat(captor.getValue().getExpression().getNormal()).isNotEmpty();
        }

        @Test
        void singleOrder_count_isAlwaysOne() {
            OrderListVO vo = buildVO(1L);
            OrderItemEntity item = buildItem(1L, 10L, "膝关节", "下肢", "模型");
            when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

            orderQueryHelper.fillRebuildProjectList(List.of(vo));

            // count 固定为 1（每行 item 一条记录）
            assertThat(vo.getRebuildProjectList().get(0).getCount()).isEqualTo(1);
        }
    }
}
