package com.yigongbao.module.design.helper;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设计工单查询辅助组件
 * 封装数据权限过滤、排序白名单、字段翻译等公共逻辑
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DesignQueryHelper {

    // ==================== 排序白名单 ====================

    /**
     * 排序字段白名单：前端字段名 → Lambda 字段引用
     * 防止 SQL 注入；只允许白名单内的字段参与排序
     */
    private static final Map<String, SFunction<OrderMainEntity, ?>> SORT_FIELD_MAP;

    static {
        Map<String, SFunction<OrderMainEntity, ?>> map = new HashMap<>();
        map.put("createTime",           OrderMainEntity::getCreateTime);
        map.put("updateTime",           OrderMainEntity::getUpdateTime);
        map.put("orderCode",            OrderMainEntity::getOrderCode);
        map.put("patientName",          OrderMainEntity::getPatientName);
        map.put("hospitalName",         OrderMainEntity::getHospitalName);
        map.put("status",               OrderMainEntity::getStatus);
        map.put("isUrgent",             OrderMainEntity::getIsUrgent);
        map.put("expectedDeliveryDate", OrderMainEntity::getExpectedDeliveryDate);
        map.put("designStartTime",      OrderMainEntity::getDesignStartTime);
        SORT_FIELD_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 设计阶段允许操作的状态白名单
     */
    private static final Set<FlowStatusEnum> ALLOWED_DESIGN_STATUSES = Set.of(
            FlowStatusEnum.DATA_AUDIT_PASSED,
            FlowStatusEnum.PENDING_DESIGN,
            FlowStatusEnum.DESIGN_IN_PROGRESS,
            FlowStatusEnum.DESIGN_COMPLETED
    );

    private final UserService userService;
    private final ConfigService configService;
    private final DictService dictService;
    private final ObjectMapper objectMapper;
    private final OrderMainMapper orderMainMapper;
    private final UserHospitalService userHospitalService;
    private final OrderMainService orderMainService;

    // ==================== 当前用户 ====================

    /**
     * 获取当前登录用户ID，未登录返回 null
     *
     * @return 当前登录用户ID
     */
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户实体，未登录或用户不存在返回 null
     *
     * @return 当前用户实体
     */
    public UserEntity getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userService.getById(userId);
    }

    // ==================== 数据权限 ====================

    /**
     * 校验当前用户是否有权查看指定订单（数据权限校验）
     * 用于查询类接口入口，防止越权查询他人工单数据
     *
     * @param orderId 订单ID
     * @throws BusinessException 无权限时抛出 ORDER_NOT_FOUND
     */
    public void checkOrderReadable(Long orderId) {
        Long currentUserId = getCurrentUserId();
        UserEntity currentUser = getCurrentUser();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId);
        buildDataScopeCondition(wrapper, currentUser, scopeType);
        if (orderMainMapper.selectCount(wrapper) == 0) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
    }

    /**
     * 根据数据范围类型向查询条件注入数据权限过滤
     * <p>
     * 设计工单按 designer_id 过滤，与订单列表按 create_by 过滤的语义不同：
     * - SELF：designer_id = 当前用户ID
     * - DEPT：designer_id IN (同部门所有用户ID)
     * - ORG：designer_id IN (同机构所有用户ID)
     * - HOSPITALS：降级为 SELF（设计师不按医院分配）
     * - ALL：不限制
     *
     * @param wrapper     查询条件构建器
     * @param currentUser 当前用户实体
     * @param scopeType   数据范围类型枚举
     */
    public void buildDataScopeCondition(LambdaQueryWrapper<OrderMainEntity> wrapper,
                                        UserEntity currentUser,
                                        DataScopeTypeEnum scopeType) {
        if (currentUser == null) {
            // 用户信息获取失败，兜底返回空列表
            log.warn("当前用户信息为空，数据权限过滤返回空列表");
            wrapper.apply("1 = 0");
            return;
        }
        Long currentUserId = currentUser.getId();

        switch (scopeType) {
            case SELF:
                // 仅看分配给自己的工单
                wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                break;
            case DEPT:
                // 看同部门所有设计师的工单
                Long deptId = currentUser.getDeptId();
                if (deptId != null) {
                    List<Long> deptUserIds = userService.listUserIdsByDeptId(deptId);
                    if (deptUserIds.isEmpty()) {
                        log.warn("DEPT 范围下部门无成员，返回空列表，deptId={}", deptId);
                        wrapper.apply("1 = 0");
                    } else {
                        wrapper.in(OrderMainEntity::getDesignerId, deptUserIds);
                    }
                } else {
                    // 用户未配置部门，降级为 SELF
                    log.warn("DEPT 类型用户未配置部门，降级为 SELF，userId={}", currentUserId);
                    wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                }
                break;
            case ORG:
                // 看同机构所有设计师的工单
                Long orgId = currentUser.getOrgId();
                if (orgId != null) {
                    List<Long> orgUserIds = userService.listUserIdsByOrgId(orgId);
                    if (orgUserIds.isEmpty()) {
                        log.warn("ORG 范围下机构无成员，返回空列表，orgId={}", orgId);
                        wrapper.apply("1 = 0");
                    } else {
                        wrapper.in(OrderMainEntity::getDesignerId, orgUserIds);
                    }
                } else {
                    log.warn("ORG 类型用户无所属机构，降级为 SELF，userId={}", currentUserId);
                    wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                }
                break;
            case HOSPITALS:
                // 设计师不按医院分配，静默降级为 SELF
                log.info("HOSPITALS 数据范围降级为 SELF（设计工单不按医院分配），userId={}", currentUserId);
                wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                break;
            case ALL:
                // 不做任何限制
                break;
        }
    }

    // ==================== 排序 ====================

    /**
     * 向查询条件追加动态排序
     * <p>
     * sortField 不在白名单时静默降级为 createTime，记录 warn 日志（防 SQL 注入）
     *
     * @param wrapper   查询条件构建器
     * @param sortField 前端传入的排序字段名，可为 null
     * @param sortOrder 前端传入的排序方向 "ASC"/"DESC"，可为 null
     */
    public void applySort(LambdaQueryWrapper<OrderMainEntity> wrapper,
                          String sortField,
                          String sortOrder) {
        SFunction<OrderMainEntity, ?> column = null;
        if (StrUtil.isNotBlank(sortField)) {
            column = SORT_FIELD_MAP.get(sortField);
            if (column == null) {
                log.warn("不支持的排序字段，已降级为默认排序，sortField={}", sortField);
            }
        }
        // sortField 为空或不在白名单中时降级为 createTime
        if (column == null) {
            column = OrderMainEntity::getCreateTime;
        }
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    // ==================== 列配置 ====================

    /**
     * 获取当前用户的列配置（用户个人配置 > 系统默认配置）
     *
     * @return 设计列配置 VO
     */
    public DesignColumnConfigVO getColumnConfig() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            UserEntity user = userService.getById(currentUserId);
            if (user != null && StrUtil.isNotBlank(user.getDesignColumnSettings())) {
                try {
                    return objectMapper.readValue(user.getDesignColumnSettings(), DesignColumnConfigVO.class);
                } catch (JsonProcessingException e) {
                    log.warn("解析用户设计列配置失败，降级为系统默认，userId={}", currentUserId, e);
                }
            }
        }
        return getSystemDefaultColumnConfig();
    }

    /**
     * 获取系统默认列配置
     *
     * @return 系统列配置 VO，配置为空或解析失败返回 null
     */
    public DesignColumnConfigVO getSystemDefaultColumnConfig() {
        String configJson = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("系统默认设计列配置为空");
            return null;
        }
        try {
            return objectMapper.readValue(configJson, DesignColumnConfigVO.class);
        } catch (JsonProcessingException e) {
            log.error("解析系统设计列配置失败", e);
            return null;
        }
    }

    // ==================== 展示字段翻译 ====================

    /**
     * 将订单类型数字值翻译为中文名称
     *
     * @param orderType 1=医疗器械，2=非医疗器械
     * @return 中文名称
     */
    public String getOrderTypeName(Integer orderType) {
        if (orderType == null) return null;
        return switch (orderType) {
            case 1 -> "医疗器械";
            case 2 -> "非医疗器械";
            default -> null;
        };
    }

    /**
     * 将实体交付标识翻译为中文名称
     *
     * @param needsPhysicalDelivery 0=否，1=是
     * @return 中文名称
     */
    public String getNeedsPhysicalDeliveryName(Integer needsPhysicalDelivery) {
        if (needsPhysicalDelivery == null) return null;
        return needsPhysicalDelivery == 1 ? "是" : "否";
    }

    /**
     * 将性别字典码翻译为中文名称
     *
     * @param gender 性别字典码（10.1=男，10.2=女）
     * @return 中文名称
     */
    public String getGenderName(String gender) {
        if (StrUtil.isBlank(gender)) return null;
        return switch (gender) {
            case DictCodeConstants.PATIENT_GENDER_MALE -> "男";
            case DictCodeConstants.PATIENT_GENDER_FEMALE -> "女";
            default -> null;
        };
    }

    /**
     * 通过字典服务将业务类型字典码翻译为字典名称
     *
     * @param dictCode 字典码
     * @return 字典名称
     */
    public String getDictName(String dictCode) {
        if (StrUtil.isBlank(dictCode)) return null;
        var dict = dictService.getByDictCode(dictCode);
        return dict != null ? dict.getDictName() : null;
    }

    /**
     * 将阶段值翻译为阶段中文名称
     *
     * @param phase 阶段值
     * @return 阶段名称
     */
    public String getPhaseName(Integer phase) {
        FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
        return phaseEnum != null ? phaseEnum.getName() : null;
    }

    /**
     * 将状态值翻译为状态中文名称
     *
     * @param status 状态值
     * @return 状态名称
     */
    public String getStatusName(Integer status) {
        FlowStatusEnum statusEnum = FlowStatusEnum.getByValue(status);
        return statusEnum != null ? statusEnum.getName() : null;
    }

    // ==================== 设计阶段公共校验 ====================

    /**
     * 校验订单存在且处于可操作的设计阶段
     * 允许的状态：数据审核通过、待设计、设计中、设计完成、设计审核不通过
     */
    public OrderMainEntity checkDesignPhase(Long orderId) {
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        FlowStatusEnum status = FlowStatusEnum.getByValue(order.getStatus());
        if (status == null || !ALLOWED_DESIGN_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        return order;
    }

    /**
     * 校验当前用户是该订单的指定设计师（仅对 designer 角色生效）
     * 设计师管理员、系统管理员、超管等角色跳过本人校验。
     */
    public void checkIsAssignedDesigner(OrderMainEntity order) {
        if (!StpUtil.hasPermission("design:EditFile")) {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            if (!currentUserId.equals(order.getDesignerId())) {
                throw new BusinessException(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED);
            }
        }
    }
}
