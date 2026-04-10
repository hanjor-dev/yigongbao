package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyPageQueryDTO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.entity.OrderModifyApplyEntity;
import com.yigongbao.module.order.enums.ModifyApplyStatusEnum;
import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.mapper.OrderModifyApplyMapper;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.vo.modify.ApplicableModifyTypesVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderModifyApplyServiceImpl 单元测试
 * 覆盖：创建申请、撤回申请、审核申请、执行修改（基础信息/重建项目/影像文件）、留痕记录等核心场景
 *
 * @author hanjor
 * @date 2026-04-08
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderModifyApplyServiceImplTest {

    @Mock private OrderModifyApplyMapper orderModifyApplyMapper;
    @Mock private OrderModificationLogMapper orderModificationLogMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderFileMapper orderFileMapper;
    @Mock private HospitalMapper hospitalMapper;
    @Mock private OrderDataValidator orderDataValidator;
    @Mock private FileService fileService;
    @Mock private ConfigService configService;
    @Mock private UserService userService;

    @InjectMocks
    private OrderModifyApplyServiceImpl service;

    private static final long ORDER_ID = 100L;
    private static final long APPLY_ID = 1L;
    private static final long USER_ID = 5L;
    private static final String ORDER_CODE = "ORD-20260408-000001";

    @BeforeEach
    void setUp() {
        // 默认用户信息
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setRealName("测试用户");
        when(userService.getById(anyLong())).thenReturn(user);

        // 默认字段配置为空（加载失败时使用默认配置）
        when(configService.getConfigValue(any())).thenReturn(null);
    }

    // ==================== 辅助方法 ====================

    private OrderMainEntity buildOrder(int phase) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(ORDER_ID);
        order.setOrderCode(ORDER_CODE);
        order.setPhase(phase);
        order.setStatus(2001);
        order.setHospitalId(10L);
        order.setHospitalName("测试医院");
        order.setPatientName("张三");
        order.setPatientAge(30);
        return order;
    }

    private OrderModifyApplyEntity buildApply(String status, String typeCodes) {
        OrderModifyApplyEntity apply = new OrderModifyApplyEntity();
        apply.setId(APPLY_ID);
        apply.setOrderId(ORDER_ID);
        apply.setOrderCode(ORDER_CODE);
        apply.setApplyTypeCodes(typeCodes);
        apply.setApplyTypeNames(ModifyApplyTypeEnum.toNamesText(typeCodes));
        apply.setApplyReason("测试申请原因");
        apply.setStatus(status);
        apply.setApplicantId(USER_ID);
        apply.setApplicantName("测试用户");
        return apply;
    }

    private OrderItemEntity buildOrderItem(long id, long projectId) {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(id);
        item.setOrderId(ORDER_ID);
        item.setOrderCode(ORDER_CODE);
        item.setProjectId(projectId);
        item.setProjectName("项目" + projectId);
        item.setBodyPartId(1L);
        item.setBodyPartName("颈椎");
        item.setSortOrder(1);
        return item;
    }

    // ==================== getApplicableTypes ====================

    @Nested
    class GetApplicableTypesTests {

        @Test
        void 订单不存在_抛出ORDER_NOT_FOUND异常() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.getApplicableTypes(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        void 阶段不允许_返回空类型列表且reason为PHASE_NOT_ALLOWED() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(30)); // 打印阶段

            ApplicableModifyTypesVO result = service.getApplicableTypes(ORDER_ID);

            assertThat(result.getAllowedTypes()).isEmpty();
            assertThat(result.getReason()).isEqualTo(ApplicableModifyTypesVO.REASON_PHASE_NOT_ALLOWED);
            assertThat(result.getPendingApplyId()).isNull();
        }

        @Test
        void 已有待审核申请_返回空类型列表且携带pendingApplyId() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
            OrderModifyApplyEntity pendingApply = new OrderModifyApplyEntity();
            pendingApply.setId(999L);
            pendingApply.setStatus(ModifyApplyStatusEnum.PENDING.getCode());
            when(orderModifyApplyMapper.selectOne(any())).thenReturn(pendingApply);

            ApplicableModifyTypesVO result = service.getApplicableTypes(ORDER_ID);

            assertThat(result.getAllowedTypes()).isEmpty();
            assertThat(result.getReason()).isEqualTo(ApplicableModifyTypesVO.REASON_PENDING_EXISTS);
            assertThat(result.getPendingApplyId()).isEqualTo(999L);
        }

        @Test
        void 订单阶段_返回可申请全部类型() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
            when(orderModifyApplyMapper.selectOne(any())).thenReturn(null);

            ApplicableModifyTypesVO result = service.getApplicableTypes(ORDER_ID);

            assertThat(result.getReason()).isNull();
            assertThat(result.getAllowedTypes()).containsExactlyInAnyOrder("14.1", "14.2", "14.3");
        }

        @Test
        void 设计阶段_只返回重建项目类型() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(20));
            when(orderModifyApplyMapper.selectOne(any())).thenReturn(null);

            ApplicableModifyTypesVO result = service.getApplicableTypes(ORDER_ID);

            assertThat(result.getReason()).isNull();
            assertThat(result.getAllowedTypes()).containsExactly("14.3");
        }
    }

    // ==================== createApply ====================

    @Nested
    class CreateApplyTests {

        @Test
        void 正常创建申请_返回VO() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                when(orderModifyApplyMapper.selectCount(any())).thenReturn(0L);
                when(orderModifyApplyMapper.insert(any(OrderModifyApplyEntity.class))).thenReturn(1);

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("14.1,14.3");
                dto.setApplyReason("患者信息变更");

                ModifyApplyVO vo = service.createApply(ORDER_ID, dto);

                assertThat(vo).isNotNull();
                assertThat(vo.getApplyTypeCodes()).isEqualTo("14.1,14.3");
                assertThat(vo.getApplyTypeNames()).isEqualTo("基础信息、重建项目");
                assertThat(vo.getStatus()).isEqualTo(ModifyApplyStatusEnum.PENDING.getCode());
                verify(orderModifyApplyMapper).insert(any(OrderModifyApplyEntity.class));
            }
        }

        @Test
        void 订单不存在_抛出异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(null);

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("14.1");
                dto.setApplyReason("原因");

                assertThatThrownBy(() -> service.createApply(ORDER_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不存在");
            }
        }

        @Test
        void 已有待审核申请_抛出ORDER_MODIFY_APPLY_EXISTS异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                when(orderModifyApplyMapper.selectCount(any())).thenReturn(1L);

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("14.1");
                dto.setApplyReason("原因");

                assertThatThrownBy(() -> service.createApply(ORDER_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_EXISTS.getCode()));
            }
        }

        @Test
        void 设计阶段申请基础信息类型_抛出ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(20));
                when(orderModifyApplyMapper.selectCount(any())).thenReturn(0L);

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("14.1"); // 设计阶段不允许基础信息
                dto.setApplyReason("原因");

                assertThatThrownBy(() -> service.createApply(ORDER_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE.getCode()));
            }
        }

        @Test
        void 无效申请类型_抛出异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                when(orderModifyApplyMapper.selectCount(any())).thenReturn(0L);

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("99.9"); // 不存在的类型
                dto.setApplyReason("原因");

                assertThatThrownBy(() -> service.createApply(ORDER_ID, dto))
                        .isInstanceOf(BusinessException.class);
            }
        }

        @Test
        void 其他阶段创建申请_抛出ORDER_NOT_APPLICABLE_STATUS异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(50)); // 质检阶段

                CreateModifyApplyDTO dto = new CreateModifyApplyDTO();
                dto.setApplyTypes("14.1");
                dto.setApplyReason("原因");

                assertThatThrownBy(() -> service.createApply(ORDER_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_NOT_APPLICABLE_STATUS.getCode()));
            }
        }
    }

    // ==================== withdrawApply ====================

    @Nested
    class WithdrawApplyTests {

        @Test
        void 正常撤回申请() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1"));

                service.withdrawApply(APPLY_ID);

                verify(orderModifyApplyMapper).deleteById(APPLY_ID);
            }
        }

        @Test
        void 申请不存在_抛出异常() {
            when(orderModifyApplyMapper.selectById(APPLY_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.withdrawApply(APPLY_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND.getCode()));
        }

        @Test
        void 申请状态非PENDING_抛出异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.1"));

                assertThatThrownBy(() -> service.withdrawApply(APPLY_ID))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR.getCode()));
            }
        }

        @Test
        void 非申请人撤回_抛出ORDER_MODIFY_APPLY_NOT_MINE异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L); // 其他用户
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1"));

                assertThatThrownBy(() -> service.withdrawApply(APPLY_ID))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_MINE.getCode()));
            }
        }
    }

    // ==================== auditApply ====================

    @Nested
    class AuditApplyTests {

        @Test
        void 同意申请_状态变更为APPROVED() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                OrderModifyApplyEntity apply = buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1");
                when(orderModifyApplyMapper.selectById(APPLY_ID)).thenReturn(apply);

                AuditModifyApplyDTO dto = new AuditModifyApplyDTO();
                dto.setAction("APPROVE");

                service.auditApply(APPLY_ID, dto);

                assertThat(apply.getStatus()).isEqualTo(ModifyApplyStatusEnum.APPROVED.getCode());
                assertThat(apply.getAuditorId()).isEqualTo(USER_ID);
                assertThat(apply.getAuditTime()).isNotNull();
                verify(orderModifyApplyMapper).updateById(apply);
            }
        }

        @Test
        void 拒绝申请_状态变更为REJECTED且保存驳回原因() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                OrderModifyApplyEntity apply = buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1");
                when(orderModifyApplyMapper.selectById(APPLY_ID)).thenReturn(apply);

                AuditModifyApplyDTO dto = new AuditModifyApplyDTO();
                dto.setAction("REJECT");
                dto.setRejectReason("信息不符合要求");

                service.auditApply(APPLY_ID, dto);

                assertThat(apply.getStatus()).isEqualTo(ModifyApplyStatusEnum.REJECTED.getCode());
                assertThat(apply.getRejectReason()).isEqualTo("信息不符合要求");
            }
        }

        @Test
        void 拒绝时未填驳回原因_抛出ORDER_MODIFY_REJECT_REASON_REQUIRED异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1"));

                AuditModifyApplyDTO dto = new AuditModifyApplyDTO();
                dto.setAction("REJECT");
                // rejectReason 为空

                assertThatThrownBy(() -> service.auditApply(APPLY_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_REJECT_REASON_REQUIRED.getCode()));
            }
        }

        @Test
        void 申请已处理_抛出ORDER_MODIFY_APPLY_ALREADY_PROCESSED异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.1"));

                AuditModifyApplyDTO dto = new AuditModifyApplyDTO();
                dto.setAction("APPROVE");

                assertThatThrownBy(() -> service.auditApply(APPLY_ID, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_ALREADY_PROCESSED.getCode()));
            }
        }
    }

    // ==================== executeModification ====================

    @Nested
    class ExecuteModificationTests {

        @Test
        void 申请状态非APPROVED_抛出异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.PENDING.getCode(), "14.1"));

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("patientName", "李四");

                assertThatThrownBy(() -> service.executeModification(APPLY_ID, modifications))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR.getCode()));
            }
        }

        @Test
        void 申请不存在_抛出ORDER_MODIFY_APPLY_NOT_FOUND异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(orderModifyApplyMapper.selectById(APPLY_ID)).thenReturn(null);

                Map<String, Object> modifications = new HashMap<>();

                assertThatThrownBy(() -> service.executeModification(APPLY_ID, modifications))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND.getCode()));
            }
        }

        @Test
        void 基础信息修改_患者姓名变更_记录留痕() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.1"));
                OrderMainEntity order = buildOrder(10);
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("patientName", "李四（更新）");

                service.executeModification(APPLY_ID, modifications);

                // 验证订单更新
                verify(orderMainMapper).updateById(any(OrderMainEntity.class));
                // 验证留痕记录
                verify(orderModificationLogMapper).insert(any(OrderModificationLogEntity.class));
            }
        }

        @Test
        void 重建项目修改_新增项目_记录留痕() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.3"));
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                // 旧 items 为空
                when(orderItemMapper.selectList(any())).thenReturn(List.of());

                Map<String, Object> item1 = new HashMap<>();
                item1.put("bodyPartId", 1);
                item1.put("projectId", 10);
                item1.put("projectDesc", "新项目");

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("items", List.of(item1));

                service.executeModification(APPLY_ID, modifications);

                // 验证新 item 插入
                verify(orderItemMapper).insert(any(OrderItemEntity.class));
                // 验证留痕记录
                verify(orderModificationLogMapper).insert(any(OrderModificationLogEntity.class));
            }
        }

        @Test
        void 重建项目修改_删除旧项目_记录留痕() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.3"));
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                // 旧 items 有一项
                when(orderItemMapper.selectList(any()))
                        .thenReturn(List.of(buildOrderItem(10L, 100L)));

                // items 为空列表（删除全部旧项目）
                Map<String, Object> modifications = new HashMap<>();
                modifications.put("items", List.of());

                service.executeModification(APPLY_ID, modifications);

                verify(orderItemMapper).deleteById(10L);
                // 留痕：删除
                verify(orderModificationLogMapper).insert(any(OrderModificationLogEntity.class));
            }
        }

        @Test
        void 重建项目修改_传入不属于当前订单的itemId_抛出异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.3"));
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                // 旧 items 只有 id=10
                when(orderItemMapper.selectList(any()))
                        .thenReturn(List.of(buildOrderItem(10L, 100L)));

                // 尝试更新 id=999（不属于该订单）
                Map<String, Object> badItem = new HashMap<>();
                badItem.put("orderItemId", 999);
                badItem.put("projectId", 100);

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("items", List.of(badItem));

                assertThatThrownBy(() -> service.executeModification(APPLY_ID, modifications))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不属于当前订单");
            }
        }

        @Test
        void 影像文件修改_文件不存在_抛出ORDER_FILE_NOT_FOUND异常() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.2"));
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                // 文件服务返回 0 个文件（请求了 1 个）
                when(fileService.listByIds(any())).thenReturn(List.of());

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("imageDataFileIds", List.of("file-id-1"));

                assertThatThrownBy(() -> service.executeModification(APPLY_ID, modifications))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                                .isEqualTo(ErrorCodeEnum.ORDER_FILE_NOT_FOUND.getCode()));
            }
        }

        @Test
        void 影像文件修改_正常替换_记录留痕() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderModifyApplyMapper.selectById(APPLY_ID))
                        .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.2"));
                when(orderMainMapper.selectById(ORDER_ID)).thenReturn(buildOrder(10));
                // 文件存在
                FileVO fileVO = new FileVO();
                fileVO.setId("file-id-1");
                when(fileService.listByIds(any())).thenReturn(List.of(fileVO));
                // 旧文件关联为空
                when(orderFileMapper.selectList(any())).thenReturn(List.of());

                Map<String, Object> modifications = new HashMap<>();
                modifications.put("imageDataFileIds", List.of("file-id-1"));

                service.executeModification(APPLY_ID, modifications);

                verify(orderFileMapper).insert(any(OrderFileEntity.class));
                verify(orderModificationLogMapper).insert(any(OrderModificationLogEntity.class));
            }
        }
    }

    // ==================== listModificationLogs ====================

    @Nested
    class ListModificationLogsTests {

        @Test
        void 分页查询留痕记录_返回分页结果() {
            Page<OrderModificationLogEntity> page = new Page<>(1, 10);
            when(orderModificationLogMapper.selectPage(any(), any())).thenReturn(page);

            ModificationLogPageQueryDTO dto = new ModificationLogPageQueryDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);

            IPage<com.yigongbao.module.order.vo.modify.ModificationLogVO> result =
                    service.listModificationLogs(ORDER_ID, dto);

            assertThat(result).isNotNull();
        }
    }

    // ==================== listMyApplies ====================

    @Nested
    class ListMyAppliesTests {

        @Test
        void 分页查询我的申请_返回分页结果() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Page<OrderModifyApplyEntity> page = new Page<>(1, 10);
                when(orderModifyApplyMapper.selectPage(any(), any())).thenReturn(page);

                ModifyApplyPageQueryDTO dto = new ModifyApplyPageQueryDTO();
                dto.setPageNum(1);
                dto.setPageSize(10);

                IPage<com.yigongbao.module.order.vo.modify.ModifyApplyListVO> result =
                        service.listMyApplies(dto);

                assertThat(result).isNotNull();
            }
        }
    }
}
