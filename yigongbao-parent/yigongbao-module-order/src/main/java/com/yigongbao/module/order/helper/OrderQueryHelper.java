package com.yigongbao.module.order.helper;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单查询公共辅助组件
 * 封装 OrderMainServiceImpl 和 OrderExportServiceImpl 共用的查询逻辑，
 * 消除两个 Service 间的代码重复（P2-4）
 *
 * @author hanjor
 * @date 2026-04-07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHelper {

    // ==================== 排序白名单 ====================

    /**
     * 排序字段白名单：前端字段名 → Lambda 字段引用
     * 防止 SQL 注入；LambdaQueryWrapper 不支持字符串字段名，必须映射到 SFunction
     */
    private static final Map<String, SFunction<OrderMainEntity, ?>> SORT_FIELD_MAP;

    static {
        Map<String, SFunction<OrderMainEntity, ?>> map = new HashMap<>();
        // 列表页常见排序字段，按字段名 A-Z 排列
        map.put("createTime",           OrderMainEntity::getCreateTime);
        map.put("updateTime",           OrderMainEntity::getUpdateTime);
        map.put("orderCode",            OrderMainEntity::getOrderCode);
        map.put("patientName",          OrderMainEntity::getPatientName);
        map.put("doctorName",           OrderMainEntity::getDoctorName);
        map.put("hospitalName",         OrderMainEntity::getHospitalName);
        map.put("areaName",             OrderMainEntity::getAreaName);
        map.put("businessType",         OrderMainEntity::getBusinessType);
        map.put("estimatedCost",        OrderMainEntity::getEstimatedCost);
        map.put("expectedDeliveryDate", OrderMainEntity::getExpectedDeliveryDate);
        // 状态排序：支持按流程状态排序，便于查看同类订单聚集
        map.put("status",               OrderMainEntity::getStatus);
        map.put("isUrgent",             OrderMainEntity::getIsUrgent);
        SORT_FIELD_MAP = Collections.unmodifiableMap(map);
    }

    private final UserService userService;
    private final UserHospitalService userHospitalService;
    private final ConfigService configService;
    private final DictService dictService;
    private final ObjectMapper objectMapper;
    private final OrderItemMapper orderItemMapper;

    // ==================== 当前用户 ====================

    /**
     * 获取当前登录用户ID，未登录返回 null
     *
     * @return 当前登录用户ID，未登录或会话无效时返回 null
     */
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            // 未登录时 Sa-Token 会抛出异常，此处静默返回 null，不影响业务流程
            return null;
        }
    }

    /**
     * 获取当前登录用户的机构ID，未登录或无机构返回 null
     *
     * @return 当前用户所属机构ID
     */
    public Long getCurrentUserOrgId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        UserEntity user = userService.getById(userId);
        return user != null ? user.getOrgId() : null;
    }

    /**
     * 获取当前登录用户的所属部门ID，用户未配置部门时返回 null
     *
     * @return 当前用户所属部门ID
     */
    public Long getCurrentUserDeptId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        UserEntity user = userService.getById(userId);
        return user != null ? user.getDeptId() : null;
    }

    // ==================== 数据权限 ====================

    /**
     * 根据数据范围类型向查询条件注入数据权限过滤
     * <p>
     * 数据范围类型说明：
     * - SELF：只看自己创建的订单
     * - DEPT：只看同部门成员创建的订单（按 operator_dept_id 过滤）
     * - HOSPITALS：只看自己关联医院范围内的订单
     * - ORG：只看同机构下所有订单
     * - ALL：不受限制，查看所有订单
     *
     * @param wrapper       查询条件构建器
     * @param currentUserId 当前用户ID
     * @param scopeType    数据范围类型枚举
     */
    public void buildDataScopeCondition(LambdaQueryWrapper<OrderMainEntity> wrapper,
                                        Long currentUserId,
                                        DataScopeTypeEnum scopeType) {
        switch (scopeType) {
            case SELF:
                // 仅看自己作为操作员创建的订单
                wrapper.eq(currentUserId != null, OrderMainEntity::getOperatorId, currentUserId);
                break;
            case HOSPITALS:
                // 看自己关联的医院范围内 + 自己创建的订单
                List<Long> hospitalIds = userHospitalService.getHospitalIdsByUserId(currentUserId);
                if (hospitalIds.isEmpty()) {
                    // 用户未关联任何医院（理论上不应发生），返回空列表
                    log.info("用户无权访问任何医院，返回空列表，userId={}", currentUserId);
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.in(OrderMainEntity::getHospitalId, hospitalIds)
                           .eq(OrderMainEntity::getOperatorId, currentUserId);
                }
                break;
            case ORG:
                // 看同机构下所有订单（可能含多个医院）
                Long orgId = getCurrentUserOrgId();
                if (orgId != null) {
                    wrapper.eq(OrderMainEntity::getOrgId, orgId);
                } else {
                    // 用户无所属机构，兜底返回空列表，避免泄露全量数据
                    log.warn("用户无所属机构，ORG 数据范围返回空列表，userId={}", currentUserId);
                    wrapper.apply("1 = 0");
                }
                break;
            case DEPT:
                // 按提单人部门过滤，仅能查看同部门成员创建的订单
                Long deptId = getCurrentUserDeptId();
                if (deptId != null) {
                    wrapper.eq(OrderMainEntity::getOperatorDeptId, deptId);
                } else {
                    // 用户未配置部门，降级为仅看自己，避免泄露全量数据
                    log.warn("DEPT 类型用户未配置部门，降级为 SELF，userId={}", currentUserId);
                    if (currentUserId != null) {
                        wrapper.eq(OrderMainEntity::getCreateBy, currentUserId);
                    } else {
                        // currentUserId 也为 null（会话失效等异常情况），硬兜底返回空列表
                        log.warn("DEPT 降级 SELF 但 currentUserId 也为 null，返回空列表");
                        wrapper.apply("1 = 0");
                    }
                }
                break;
            case ALL:
                // 不做任何数据范围限制
                break;
        }
    }

    // ==================== 排序 ====================

    /**
     * 向查询条件追加动态排序
     * <p>
     * 排序规则：
     * - sortField 不在白名单时，静默降级为 createTime，记录 warn 日志（防 SQL 注入）
     * - sortField / sortOrder 均为 null 时，使用默认 createTime DESC
     * - sortOrder 大小写不敏感
     *
     * @param wrapper   查询条件构建器
     * @param sortField 前端传入的排序字段名，可为 null
     * @param sortOrder 前端传入的排序方向 "ASC"/"DESC"，可为 null
     */
    public void applySort(LambdaQueryWrapper<OrderMainEntity> wrapper,
                          String sortField,
                          String sortOrder) {
        SFunction<OrderMainEntity, ?> column = null;
        // 白名单命中后才做排序；不命中时 column 保持 null，静默降级为 createTime
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
        // 默认降序，最新创建的订单排在最前
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    // ==================== 列表 VO 转换 ====================

    /**
     * 将订单主表实体转换为列表 VO，并填充所有展示字段
     * <p>
     * 转换策略：字段值直接复制，展示字段（xxxName）通过辅助方法翻译
     *
     * @param entity 订单主表实体
     * @return 订单列表 VO
     */
    public OrderListVO toOrderListVO(OrderMainEntity entity) {
        OrderListVO vo = new OrderListVO();
        // 基础字段复制
        vo.setId(entity.getId());
        vo.setOrderCode(entity.getOrderCode());
        // 订单类型翻译：1=医疗器械，2=非医疗器械
        vo.setOrderType(entity.getOrderType());
        vo.setOrderTypeName(getOrderTypeName(entity.getOrderType()));
        // 是否需要实体交付翻译
        vo.setNeedsPhysicalDelivery(entity.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        // 业务类型翻译：通过字典服务查询 dict_code 对应的字典名称
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessTypeName(getDictName(entity.getBusinessType()));
        // 机构信息（实体中已冗余存储，直接复制）
        vo.setOrgId(entity.getOrgId());
        vo.setOrgName(entity.getOrgName());
        // 操作员信息
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOperatorPhone(entity.getOperatorPhone());
        // 医院与地区（实体中已冗余存储完整路径）
        vo.setHospitalId(entity.getHospitalId());
        vo.setHospitalName(entity.getHospitalName());
        vo.setAreaId(entity.getAreaId());
        vo.setAreaName(entity.getAreaName());
        vo.setFullAreaName(entity.getFullAreaName());
        // 医院科室
        vo.setHospitalDeptId(entity.getHospitalDeptId());
        vo.setHospitalDeptName(entity.getHospitalDeptName());
        // 提单人部门与医生
        vo.setOperatorDeptId(entity.getOperatorDeptId());
        vo.setOperatorDeptName(entity.getOperatorDeptName());
        vo.setDoctorId(entity.getDoctorId());
        vo.setDoctorName(entity.getDoctorName());
        vo.setDoctorPhone(entity.getDoctorPhone());
        // 患者信息
        vo.setPatientName(entity.getPatientName());
        vo.setPatientAge(entity.getPatientAge());
        // 性别通过字典常量翻译（不查库，直接匹配）
        vo.setPatientGender(entity.getPatientGender());
        vo.setPatientGenderName(getGenderName(entity.getPatientGender()));
        // 邮寄与加急标识
        vo.setIsUrgent(entity.getIsUrgent());
        vo.setIsPostal(entity.getIsPostal());
        vo.setPostalAddress(entity.getPostalAddress());
        // 处理人与时效
        vo.setDesignerId(entity.getDesignerId());
        vo.setDesignerName(entity.getDesignerName());
        vo.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        vo.setEstimatedCost(entity.getEstimatedCost());
        vo.setDataEvaluationOpinion(entity.getDataEvaluationOpinion());
        // 阶段和状态通过流程枚举翻译（列表页直接展示中文名，避免前端再查枚举映射）
        vo.setPhase(entity.getPhase());
        vo.setPhaseName(getPhaseName(entity.getPhase()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        // 时间戳
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 批量填充订单列表 VO 的重建项目信息
     * <p>
     * 实现策略：一次性查出所有订单的全部明细，按 orderId 分组后再逐条填充。
     * 避免在循环中逐条查询明细导致的 N+1 问题。
     *
     * @param voList 订单列表
     */
    public void fillRebuildProjectList(List<OrderListVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        // 提取所有订单ID
        List<Long> orderIds = voList.stream()
                .map(OrderListVO::getId)
                .collect(Collectors.toList());
        // 一次性查询所有相关明细，过滤已删除记录
        List<OrderItemEntity> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .in(OrderItemEntity::getOrderId, orderIds)
                        .eq(OrderItemEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        if (allItems.isEmpty()) {
            return;
        }
        // 按 orderId 分组，便于快速定位
        Map<Long, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        // 逐条填充订单的明细列表
        for (OrderListVO vo : voList) {
            List<OrderItemEntity> items = itemsByOrderId.get(vo.getId());
            if (items != null && !items.isEmpty()) {
                List<OrderListVO.RebuildProjectItemVO> projectList = items.stream()
                        .map(item -> {
                            OrderListVO.RebuildProjectItemVO projectItem = new OrderListVO.RebuildProjectItemVO();
                            projectItem.setProjectName(item.getProjectName());
                            projectItem.setBodyPartName(item.getBodyPartName());
                            projectItem.setCategoryCode(item.getCategoryCode());
                            projectItem.setCategoryName(item.getCategoryName());
                            // count 字段：明细表粒度为每个部位一条，明细数即为数量
                            projectItem.setCount(1);
                            projectItem.setProjectDesc(item.getProjectDesc());
                            projectItem.setFormingRequirement(item.getFormingRequirement());
                            projectItem.setOtherRequirement(item.getOtherRequirement());
                            return projectItem;
                        })
                        .collect(Collectors.toList());
                vo.setRebuildProjectList(projectList);
            }
        }
    }

    // ==================== 列配置 ====================

    /**
     * 获取当前用户的列配置（用户个人配置 > 系统默认配置）
     * <p>
     * 优先级：
     * 1. 用户表 column_settings 字段（个人自定义配置）
     * 2. 系统配置表 sys_config（ORDER_COLUMN_CONFIG key，全局默认）
     * 3. 返回 null（前端使用内置默认列）
     *
     * @return 用户列配置 VO，优先取个人配置，未配置时取系统默认，均未配置返回 null
     */
    public OrderColumnConfigVO getColumnConfig() {
        Long currentUserId = getCurrentUserId();
        // 未登录或用户不存在时，直接返回系统默认（getSystemDefaultColumnConfig 内部有兜底）
        if (currentUserId == null) {
            return getSystemDefaultColumnConfig();
        }
        UserEntity user = userService.getById(currentUserId);
        if (user == null) {
            return getSystemDefaultColumnConfig();
        }
        // 用户已配置个人列设置，优先使用
        if (StrUtil.isNotBlank(user.getOrderColumnSettings())) {
            try {
                return objectMapper.readValue(user.getOrderColumnSettings(), OrderColumnConfigVO.class);
            } catch (JsonProcessingException e) {
                // JSON 解析失败时降级为系统默认，记录警告
                log.warn("解析用户列配置失败，使用系统默认配置，userId={}", currentUserId, e);
            }
        }
        return getSystemDefaultColumnConfig();
    }

    /**
     * 获取系统默认列配置
     * 从系统配置表读取 ORDER_COLUMN_CONFIG 配置项
     *
     * @return 系统列配置 VO，配置为空或解析失败返回 null
     */
    public OrderColumnConfigVO getSystemDefaultColumnConfig() {
        String configJson = configService.getConfigValue(SystemConfigKeyEnum.ORDER_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("系统默认列配置为空");
            return null;
        }
        try {
            return objectMapper.readValue(configJson, OrderColumnConfigVO.class);
        } catch (JsonProcessingException e) {
            log.error("解析系统列配置失败", e);
            return null;
        }
    }

    // ==================== 展示字段辅助方法 ====================

    /**
     * 填充订单详情 VO 的显示名称字段
     * 直接翻译 entity 中的枚举/字典码，不走 toOrderListVO 完整路径
     *
     * 注意：OrderDetailVO.businessType 用于存储人可读的字典名称（而非 dict_code），
     * 与 OrderListVO 中 businessType（dict_code）+businessTypeName（名称）的设计不同。
     *
     * @param entity 订单主表实体
     * @param vo     订单详情 VO（待填充 xxxName 字段）
     */
    public void fillDisplayNames(OrderMainEntity entity, OrderDetailVO vo) {
        vo.setOrderTypeName(getOrderTypeName(entity.getOrderType()));
        vo.setNeedsPhysicalDeliveryName(getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        vo.setPatientGenderName(getGenderName(entity.getPatientGender()));
        vo.setPhaseName(getPhaseName(entity.getPhase()));
        vo.setStatusName(getStatusName(entity.getStatus()));
        // OrderDetailVO 无独立 businessTypeName 字段，将翻译后的名称写入 businessType
        vo.setBusinessType(getDictName(entity.getBusinessType()));
    }

    /**
     * 将订单类型数字值翻译为中文名称
     *
     * @param orderType 订单类型：1=医疗器械，2=非医疗器械
     * @return 中文名称，未知值返回 null
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
     * 将是否需要实体交付数字值翻译为中文名称
     *
     * @param needsPhysicalDelivery 0=不需要，1=需要
     * @return 中文名称
     */
    public String getNeedsPhysicalDeliveryName(Integer needsPhysicalDelivery) {
        if (needsPhysicalDelivery == null) return null;
        return needsPhysicalDelivery == 1 ? "是" : "否";
    }

    /**
     * 将患者性别字典码翻译为中文名称
     * 使用字典常量而非字典表查询，避免额外的数据库开销
     *
     * @param gender 患者性别字典码（10.1=男，10.2=女）
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
     * 适用于业务类型、文件类别等存储 dict_code 而非直接存储中文的场景
     *
     * @param dictCode 字典码
     * @return 字典名称，字典不存在返回 null
     */
    public String getDictName(String dictCode) {
        if (StrUtil.isBlank(dictCode)) return null;
        var dict = dictService.getByDictCode(dictCode);
        return dict != null ? dict.getDictName() : null;
    }

    /**
     * 通过流程阶段枚举将阶段值翻译为阶段中文名称
     *
     * @param phase 阶段值（1=订单，2=设计，3=打印，4=后处理，5=质检，6=仓储，7=确认，8=完成）
     * @return 阶段名称
     */
    public String getPhaseName(Integer phase) {
        FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
        return phaseEnum != null ? phaseEnum.getName() : null;
    }

    /**
     * 通过流程状态枚举将状态值翻译为状态中文名称
     *
     * @param status 状态值（10-80 范围，每10个值对应一个阶段）
     * @return 状态名称
     */
    private String getStatusName(Integer status) {
        FlowStatusEnum statusEnum = FlowStatusEnum.getByValue(status);
        return statusEnum != null ? statusEnum.getName() : null;
    }
}
