package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyFieldConfigDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyPageQueryDTO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.entity.OrderModifyApplyEntity;
import com.yigongbao.module.order.enums.AuditActionEnum;
import com.yigongbao.module.order.enums.ModifyApplyStatusEnum;
import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.mapper.OrderModifyApplyMapper;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.vo.modify.ApplicableModifyTypesVO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyDetailVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyListVO;
import com.yigongbao.module.order.vo.modify.ModifyApplyVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;

/**
 * 订单修改申请 Service 实现类
 *
 * 【设计说明】
 * - 不注入 OrderMainService，直接注入 OrderMainMapper（规避循环依赖）
 * - 医院/科室/医生的冗余字段同步统一走 OrderDataValidator.validateAndFillForModify
 * - processInfoModification / processItemModification / processImageModification 均在同一事务内执行
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderModifyApplyServiceImpl implements OrderModifyApplyService {

    private final OrderModifyApplyMapper orderModifyApplyMapper;
    private final OrderModificationLogMapper orderModificationLogMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderFileMapper orderFileMapper;
    private final OrderDataValidator orderDataValidator;
    // 文件存在性校验
    private final FileService fileService;
    private final ConfigService configService;
    private final UserService userService;
    private final FlowFacade flowFacade;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ==================== 阶段判断 ====================

    /**
     * 获取订单当前可申请的修改类型列表
     *
     * @param orderId 订单ID
     * @return 可申请修改类型结果
     */
    @Override
    public ApplicableModifyTypesVO getApplicableTypes(Long orderId) {
        log.info("获取可申请修改类型，orderId={}", orderId);
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 校验订单阶段（仅 phase=10 或 phase=20 可申请）
        Integer phase = order.getPhase();
        if (!FlowPhaseEnum.ORDER.getValue().equals(phase)
                && !FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
            return ApplicableModifyTypesVO.forPhaseNotAllowed();
        }

        // 校验是否有待审核申请
        OrderModifyApplyEntity pendingApply = orderModifyApplyMapper.selectOne(
                new LambdaQueryWrapper<OrderModifyApplyEntity>()
                        .eq(OrderModifyApplyEntity::getOrderId, orderId)
                        .eq(OrderModifyApplyEntity::getStatus, ModifyApplyStatusEnum.PENDING.getCode())
                        .last("LIMIT 1")
        );
        if (pendingApply != null) {
            return ApplicableModifyTypesVO.forPendingExists(pendingApply.getId());
        }

        // 获取允许的申请类型
        List<String> allowedTypes;
        if (FlowPhaseEnum.ORDER.getValue().equals(phase)) {
            // 订单阶段：允许全部类型
            allowedTypes = List.of(
                    ModifyApplyTypeEnum.INFO.getDictCode(),
                    ModifyApplyTypeEnum.IMAGE.getDictCode(),
                    ModifyApplyTypeEnum.ITEM.getDictCode()
            );
        } else {
            // 设计阶段：仅允许重建项目（14.3）
            allowedTypes = List.of(ModifyApplyTypeEnum.ITEM.getDictCode());
        }

        return ApplicableModifyTypesVO.forAllowed(allowedTypes);
    }

    /**
     * 根据订单阶段判断允许的修改类型
     * <p>
     * 订单阶段（phase=10）：允许全部三种类型（14.1/14.2/14.3）<br>
     * 设计阶段（phase=20）：仅允许重建项目（14.3）<br>
     * 其他阶段：抛出异常
     *
     * @param phase 订单阶段值
     * @return 允许的类型编码集合
     */
    Set<String> determineAllowedTypesByPhase(Integer phase) {
        if (FlowPhaseEnum.ORDER.getValue().equals(phase)) {
            return Set.of(
                ModifyApplyTypeEnum.INFO.getDictCode(),
                ModifyApplyTypeEnum.IMAGE.getDictCode(),
                ModifyApplyTypeEnum.ITEM.getDictCode()
            );
        } else if (FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
            return Set.of(ModifyApplyTypeEnum.ITEM.getDictCode());
        } else {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_APPLICABLE_STATUS);
        }
    }

    /**
     * 将 ExecuteModifyDTO 转换为 Map 结构（供内部处理方法使用）
     */
    Map<String, Object> buildModificationsMap(ExecuteModifyDTO dto) {
        Map<String, Object> modifications = new HashMap<>();
        if (dto != null) {
            if (dto.getInfoFields() != null) {
                dto.getInfoFields().forEach(f -> {
                    if (StrUtil.isNotBlank(f.getField())) {
                        modifications.put(f.getField(), f.getValue());
                    }
                });
            }
            if (dto.getItems() != null) {
                List<Map<String, Object>> itemMaps = dto.getItems().stream().map(item -> {
                    Map<String, Object> m = new HashMap<>();
                    if (item.getOrderItemId() != null) {
                        m.put("orderItemId", item.getOrderItemId());
                    }
                    if (item.getFields() != null) {
                        item.getFields().forEach(f -> {
                            if (StrUtil.isNotBlank(f.getField())) {
                                m.put(f.getField(), f.getValue());
                            }
                        });
                    }
                    return m;
                }).collect(Collectors.toList());
                modifications.put("items", itemMaps);
            }
            if (dto.getImageDataFileIds() != null) {
                modifications.put("imageDataFileIds", dto.getImageDataFileIds());
            }
            if (dto.getImageReportFileIds() != null) {
                modifications.put("imageReportFileIds", dto.getImageReportFileIds());
            }
        }
        return modifications;
    }

    // ==================== 申请发起 ====================

    /**
     * 发起修改申请
     *
     * @param orderId 订单ID
     * @param dto     申请参数
     * @return 申请 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModifyApplyVO createApply(Long orderId, CreateModifyApplyDTO dto) {
        log.info("发起修改申请，orderId={}, applyTypes={}", orderId, dto.getApplyTypes());
        // 1. 校验订单存在
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 2. 校验订单阶段适用修改申请（phase=10 或 phase=20）
        Integer phase = order.getPhase();
        if (!FlowPhaseEnum.ORDER.getValue().equals(phase)
                && !FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_APPLICABLE_STATUS);
        }

        // 3. 校验申请类型有效性（枚举校验，无需 DB 查询）
        validateApplyTypes(dto.getApplyTypes());

        // 4. 校验阶段+类型联合限制（设计阶段只允许 14.3）
        validateTypeInPhase(phase, dto.getApplyTypes());

        // 5. 校验无待审核申请（应用层校验，函数索引作为数据库层兜底）
        validateNoPendingApply(orderId);

        // 6. 构建申请实体（存入前对 applyTypes 标准化：去除多余空格）
        String normalizedTypes = Arrays.stream(dto.getApplyTypes().split(","))
                .map(String::trim)
                .collect(Collectors.joining(","));
        OrderModifyApplyEntity apply = new OrderModifyApplyEntity();
        apply.setOrderId(orderId);
        apply.setOrderCode(order.getOrderCode());
        apply.setHospitalName(order.getHospitalName());
        apply.setPatientName(order.getPatientName());
        apply.setApplyTypeCodes(normalizedTypes);
        apply.setApplyTypeNames(ModifyApplyTypeEnum.toNamesText(normalizedTypes));
        apply.setApplyReason(dto.getApplyReason());
        apply.setStatus(ModifyApplyStatusEnum.PENDING.getCode());
        apply.setApplicantId(StpUtil.getLoginIdAsLong());
        apply.setApplicantName(getCurrentUserName());
        try {
            orderModifyApplyMapper.insert(apply);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_EXISTS);
        }

        log.info("发起修改申请成功，applyId={}", apply.getId());
        return toApplyVO(apply);
    }

    /**
     * 校验申请类型有效性（枚举校验，无需 DB 查询）
     */
    private void validateApplyTypes(String applyTypes) {
        if (StrUtil.isBlank(applyTypes)) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "申请类型");
        }
        for (String type : applyTypes.split(",")) {
            if (ModifyApplyTypeEnum.getByDictCode(type.trim()) == null) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "申请类型：" + type.trim());
            }
        }
    }

    /**
     * 校验申请类型是否在当前阶段允许
     * 订单阶段（phase=10）：允许全部类型；设计阶段（phase=20）：仅允许 14.3（重建项目）
     */
    private void validateTypeInPhase(Integer phase, String applyTypes) {
        if (FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
            for (String type : applyTypes.split(",")) {
                ModifyApplyTypeEnum typeEnum = ModifyApplyTypeEnum.getByDictCode(type.trim());
                if (typeEnum != null && typeEnum != ModifyApplyTypeEnum.ITEM) {
                    log.warn("设计阶段不允许申请类型：{}", type.trim());
                    throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE);
                }
            }
        }
    }

    /**
     * 校验同一订单无待审核申请（应用层兜底，函数索引为数据库层防并发）
     */
    private void validateNoPendingApply(Long orderId) {
        Long count = orderModifyApplyMapper.selectCount(
                new LambdaQueryWrapper<OrderModifyApplyEntity>()
                        .eq(OrderModifyApplyEntity::getOrderId, orderId)
                        .eq(OrderModifyApplyEntity::getStatus, ModifyApplyStatusEnum.PENDING.getCode())
        );
        if (count > 0) {
            log.warn("订单已有待审核申请，orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_EXISTS);
        }
    }

    // ==================== 申请撤回 ====================

    /**
     * 撤回申请（逻辑删除，仅申请人可撤回待审核申请）
     *
     * @param applyId 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApply(Long applyId) {
        log.info("撤回修改申请，applyId={}", applyId);
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        if (!ModifyApplyStatusEnum.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR);
        }
        if (!apply.getApplicantId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_MINE);
        }
        // 逻辑删除（MyBatis-Plus @TableLogic 会将 is_deleted 置为 1）
        orderModifyApplyMapper.deleteById(applyId);
        log.info("撤回修改申请成功，applyId={}", applyId);
    }

    // ==================== 申请审核 ====================

    /**
     * 审核申请（同意/拒绝）
     *
     * @param applyId 申请ID
     * @param dto     审核参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditApply(Long applyId, AuditModifyApplyDTO dto) {
        log.info("审核修改申请，applyId={}, action={}", applyId, dto.getAction());
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        if (!ModifyApplyStatusEnum.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_ALREADY_PROCESSED);
        }

        // 校验驳回时必填原因
        AuditActionEnum action = dto.getAction();
        if (AuditActionEnum.REJECT == action && StrUtil.isBlank(dto.getRejectReason())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_REJECT_REASON_REQUIRED);
        }

        // 更新申请状态
        Long auditorId = StpUtil.getLoginIdAsLong();
        String auditorName = getCurrentUserName();
        apply.setAuditorId(auditorId);
        apply.setAuditorName(auditorName);
        apply.setAuditTime(LocalDateTime.now());
        if (AuditActionEnum.APPROVE == action) {
            apply.setStatus(ModifyApplyStatusEnum.APPROVED.getCode());
        } else {
            apply.setStatus(ModifyApplyStatusEnum.REJECTED.getCode());
            apply.setRejectReason(dto.getRejectReason());
        }
        orderModifyApplyMapper.updateById(apply);
        log.info("审核修改申请成功，applyId={}, status={}", applyId, apply.getStatus());
    }

    // ==================== 执行修改 ====================

    /**
     * 执行订单修改（审核通过后调用）
     * 这是统一修改入口，包含基础信息/影像文件/重建项目三类修改，均在同一事务内
     *
     * @param applyId       修改申请ID
     * @param dto 修改字段 对象，只传需要修改的字段，后端根据申请类型白名单过滤
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeModification(Long applyId, ExecuteModifyDTO dto) {
        log.info("执行订单修改，applyId={}", applyId);
        Map<String, Object> modifications = buildModificationsMap(dto);

        // 1. 校验申请存在且状态为 APPROVED
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        if (!ModifyApplyStatusEnum.APPROVED.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR);
        }

        // 2. 通过 apply 反查 orderId（无需外部传入，自动保证关联一致性）
        Long orderId = apply.getOrderId();

        // 3. 解析允许的申请类型（trim 防止存储时残留空格）
        Set<String> allowedTypes = Arrays.stream(apply.getApplyTypeCodes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        // 0. apply 确认有效后再加载字段配置（避免无效 applyId 触发配置 I/O）
        ModifyApplyFieldConfigDTO fieldConfig = loadFieldConfig();

        // 3a. 严格校验：infoFields 中不得包含白名单外的字段
        if (dto != null && dto.getInfoFields() != null && !dto.getInfoFields().isEmpty()) {
            ModifyApplyFieldConfigDTO.TypeConfig infoTypeConfig =
                    fieldConfig.getTypeConfig(ModifyApplyTypeEnum.INFO.getDictCode());
            validateInfoFieldsInWhitelist(dto.getInfoFields(), infoTypeConfig);
        }

        // 3b. 完整性校验：申请了哪种类型，必须提供对应内容
        validateModificationCompleteness(allowedTypes, dto);

        // 4. 查询订单实体
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 5. 获取当前操作人
        Long modifierId = StpUtil.getLoginIdAsLong();
        String modifierName = getCurrentUserName();

        // 6. 处理基础信息修改（14.1）
        boolean infoModified = false;
        if (allowedTypes.contains(ModifyApplyTypeEnum.INFO.getDictCode())) {
            infoModified = processInfoModification(order, modifications, applyId, modifierId, modifierName, fieldConfig);
        }

        // 7. 处理重建项目修改（14.3）
        if (allowedTypes.contains(ModifyApplyTypeEnum.ITEM.getDictCode()) && modifications.containsKey("items")) {
            processItemModification(order, modifications, applyId, modifierId, modifierName, fieldConfig);
        }

        // 8. 处理影像文件修改（14.2）
        if (allowedTypes.contains(ModifyApplyTypeEnum.IMAGE.getDictCode())) {
            processImageModification(order, modifications, applyId, modifierId, modifierName);
        }

        log.info("执行订单修改成功，orderId={}, applyId={}", orderId, applyId);

        // 仅 INFO 类型修改了 order 实体字段时才回写 DB（IMAGE/ITEM 不修改 order 主表字段）
        if (infoModified) {
            orderMainMapper.updateById(order);
        }

        // 9. 将申请状态置为 COMPLETED（防止重复执行）
        apply.setStatus(ModifyApplyStatusEnum.COMPLETED.getCode());
        orderModifyApplyMapper.updateById(apply);

        // 10. 执行状态流转：将订单重新流转到"数据待审核"状态
        log.info("执行修改后触发状态流转，orderId={}, 操作人={}", orderId, modifierName);
        flowFacade.executeFlow(
                orderId,
                FlowActionEnum.RESUBMIT,
                new FlowOperator(modifierId, modifierName, "执行订单修改后重新提交审核")
        );
    }

    // ==================== 辅助方法：执行前校验 ====================

    /**
     * 严格校验 infoFields 中的字段名必须全部在 14.1 白名单内
     * <p>
     * 白名单来自 sys_config order.modify.field.config 中 "14.1".fields[].field。
     * 存在任何不在白名单内的字段名时，直接抛出业务异常，拒绝整个请求。
     *
     * @param infoFields     前端传入的基础信息字段列表
     * @param infoTypeConfig 14.1 类型配置（白名单来源）
     */
    private void validateInfoFieldsInWhitelist(List<ExecuteModifyDTO.ModifyField> infoFields,
            ModifyApplyFieldConfigDTO.TypeConfig infoTypeConfig) {
        // 配置本身缺失属于运维问题，抛 721 而非 716
        if (infoTypeConfig == null || infoTypeConfig.getFields() == null) {
            log.warn("14.1 字段配置缺失，无法执行白名单校验");
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_FIELD_CONFIG_NOT_FOUND);
        }
        Set<String> whitelist = infoTypeConfig.getFields().stream()
                .map(ModifyApplyFieldConfigDTO.FieldConfig::getField)
                .collect(Collectors.toSet());
        List<String> illegalFields = infoFields.stream()
                .map(ExecuteModifyDTO.ModifyField::getField)
                .filter(f -> !whitelist.contains(f))
                .toList();
        if (!illegalFields.isEmpty()) {
            log.warn("infoFields 包含非白名单字段，illegalFields={}", illegalFields);
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED,
                    String.join(", ", illegalFields));
        }
    }

    /**
     * 完整性校验：申请了哪种类型，提交内容中必须包含该类型对应的修改数据
     * <p>
     * 14.1：infoFields 非空<br>
     * 14.2：imageDataFileIds 或 imageReportFileIds 至少一个非 null<br>
     * 14.3：items 非 null
     *
     * @param allowedTypes 本次申请包含的类型编码集合
     * @param dto          执行修改 DTO
     */
    private void validateModificationCompleteness(Set<String> allowedTypes, ExecuteModifyDTO dto) {
        for (String typeCode : allowedTypes) {
            ModifyApplyTypeEnum typeEnum = ModifyApplyTypeEnum.getByDictCode(typeCode);
            if (typeEnum == null) {
                // 未知类型不强制校验，保持向前兼容
                continue;
            }
            if (!typeEnum.isProvided(dto)) {
                log.warn("执行修改完整性校验失败，typeCode={} 未提供修改内容", typeCode);
                throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_INCOMPLETE, typeEnum.getName());
            }
        }
    }

    // ==================== 辅助方法：基础信息修改 ====================

    /**
     * 处理基础信息修改（配置驱动：快照旧值 → 反射赋新值 → 留痕）
     * <p>
     * 完全由 sys_config order.modify.field.config 中 "14.1" 的 fields 列表驱动。
     * - 无 group 的字段：通过 BeanUtil 反射直接赋值到 order 实体
     * - group="hospital_doctor" 的字段：收集后统一调用 validateAndFillForModify 同步冗余字段
     * - 快照（赋值前后对比）用于生成变更留痕，label 从配置读取
     *
     * @return true 表示至少有一个字段值发生了变化（调用方据此决定是否执行 updateById）
     */
    private boolean processInfoModification(OrderMainEntity order, Map<String, Object> modifications,
            Long applyId, Long modifierId, String modifierName, ModifyApplyFieldConfigDTO fieldConfig) {
        ModifyApplyFieldConfigDTO.TypeConfig typeConfig =
                fieldConfig.getTypeConfig(ModifyApplyTypeEnum.INFO.getDictCode());
        if (typeConfig == null || typeConfig.getFields() == null) {
            log.warn("未获取到 14.1 字段配置，跳过基础信息修改");
            return false;
        }
        List<ModifyApplyFieldConfigDTO.FieldConfig> allFields = typeConfig.getFields();

        // 1. 快照旧值（按配置字段名逐一通过反射读取，用于留痕对比）
        List<String> allFieldNames = allFields.stream()
                .map(ModifyApplyFieldConfigDTO.FieldConfig::getField)
                .toList();
        Map<String, Object> beforeSnapshot = snapshotFields(order, allFieldNames);

        // 2. 处理 hospital_doctor 分组（需调用 validator 同步冗余字段）
        List<ModifyApplyFieldConfigDTO.FieldConfig> hospitalDoctorFields =
                typeConfig.getFieldsByGroup("hospital_doctor");
        if (!hospitalDoctorFields.isEmpty()) {
            Map<String, Object> groupValues = new HashMap<>();
            for (ModifyApplyFieldConfigDTO.FieldConfig fc : hospitalDoctorFields) {
                if (modifications.containsKey(fc.getField())) {
                    groupValues.put(fc.getField(), modifications.get(fc.getField()));
                }
            }
            if (!groupValues.isEmpty()) {
                Long hospitalId = Convert.convert(Long.class, groupValues.get("hospitalId"));
                Long doctorId = Convert.convert(Long.class, groupValues.get("doctorId"));
                String doctorName = Convert.convert(String.class, groupValues.get("doctorName"));
                String doctorPhone = Convert.convert(String.class, groupValues.get("doctorPhone"));
                boolean hasDoctorChange = doctorId != null || StrUtil.isNotBlank(doctorName);
                if (hospitalId != null || hasDoctorChange) {
                    orderDataValidator.validateAndFillForModify(
                            order, hospitalId, doctorId, doctorName, doctorPhone);
                }
            }
        }

        // 3. 处理无 group 字段（通过 BeanUtil 反射逐一赋值）
        List<ModifyApplyFieldConfigDTO.FieldConfig> normalFields = typeConfig.getFieldsByGroup(null);
        for (ModifyApplyFieldConfigDTO.FieldConfig fc : normalFields) {
            String fieldName = fc.getField();
            if (!modifications.containsKey(fieldName)) {
                continue;
            }
            Object converted = convertFieldValue(fc.getType(), modifications.get(fieldName));
            if (converted != null) {
                BeanUtil.setFieldValue(order, fieldName, converted);
            }
        }

        // 4. 快照新值（反射读取赋值后的 order，用于留痕对比）
        Map<String, Object> afterSnapshot = snapshotFields(order, allFieldNames);

        // 5. 逐字段对比快照生成留痕，返回是否有实际变化
        return recordChangesFromSnapshots(order.getId(), order.getOrderCode(), applyId,
                beforeSnapshot, afterSnapshot, allFields, modifierId, modifierName);
    }

    /**
     * 按配置 type 将原始值转换为目标类型（配置驱动字段赋值的类型安全层）
     *
     * @param type 字段类型（来自 sys_config JSON 中 fields[].type）
     * @param raw  原始值（前端传入，Jackson 反序列化为 Integer/Double/String/Boolean 等）
     * @return 转换后的值，null 时返回 null
     */
    private Object convertFieldValue(String type, Object raw) {
        if (raw == null) return null;
        return switch (StrUtil.blankToDefault(type, "text")) {
            // 数值型：switch/number 均转为 Integer（兼容 0/1 布尔开关）
            case "switch", "number" -> Convert.convert(Integer.class, raw);
            // 关联 ID 型：autocomplete 传入的是 Long 主键
            case "autocomplete" -> Convert.convert(Long.class, raw);
            // 日期时间型：支持 ISO 8601 字符串或时间戳（毫秒）
            case "datetime" -> {
                if (raw instanceof String str && StrUtil.isNotBlank(str)) {
                    try {
                        yield cn.hutool.core.date.DateUtil.parseLocalDateTime(str);
                    } catch (Exception e) {
                        log.warn("datetime 字段解析失败，raw={}", str);
                        yield null;
                    }
                } else if (raw instanceof Number num) {
                    yield java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(num.longValue()),
                            java.time.ZoneId.of("Asia/Shanghai"));
                }
                yield null;
            }
            // 文本型（text/select/textarea 等）：统一转为 String，空字符串忽略
            default -> {
                String v = Convert.convert(String.class, raw);
                yield StrUtil.isBlank(v) ? null : v;
            }
        };
    }

    /**
     * 通过 BeanUtil 反射读取实体中指定字段的当前值，返回快照 Map
     *
     * @param entity     实体对象（OrderMainEntity / OrderItemEntity）
     * @param fieldNames 字段名列表（与实体属性名保持一致）
     * @return fieldName → 当前值 的快照 Map（字段不存在时跳过）
     */
    private Map<String, Object> snapshotFields(Object entity, List<String> fieldNames) {
        Map<String, Object> snapshot = new HashMap<>();
        for (String fieldName : fieldNames) {
            try {
                Object value = BeanUtil.getFieldValue(entity, fieldName);
                snapshot.put(fieldName, value);
            } catch (Exception e) {
                // 字段不存在于实体时静默跳过（配置中可能有扩展字段尚未同步到 entity）
                log.debug("实体不含字段，跳过快照，fieldName={}", fieldName);
            }
        }
        return snapshot;
    }

    /**
     * 对比前后快照，对有变化的字段记录留痕
     * label 从 fieldConfigs 中读取，避免硬编码中文名
     *
     * @return true 表示至少检测到一个字段发生了变化
     */
    private boolean recordChangesFromSnapshots(Long orderId, String orderCode, Long applyId,
            Map<String, Object> before, Map<String, Object> after,
            List<ModifyApplyFieldConfigDTO.FieldConfig> fieldConfigs,
            Long modifierId, String modifierName) {
        // 构建 field→label 映射（O(1) 查找）
        Map<String, String> labelMap = fieldConfigs.stream()
                .collect(Collectors.toMap(
                        ModifyApplyFieldConfigDTO.FieldConfig::getField,
                        fc -> StrUtil.blankToDefault(fc.getLabel(), fc.getField()),
                        (a, b) -> a));
        boolean hasChange = false;
        for (String fieldName : after.keySet()) {
            Object oldValue = before.get(fieldName);
            Object newValue = after.get(fieldName);
            if (newValue != null && !Objects.equals(oldValue, newValue)) {
                String label = labelMap.getOrDefault(fieldName, fieldName);
                recordModificationLog(orderId, orderCode, applyId,
                        fieldName, label, oldValue, newValue, modifierId, modifierName);
                hasChange = true;
            }
        }
        return hasChange;
    }

    // ==================== 辅助方法：重建项目修改 ====================

    /**
     * 处理重建项目修改（配置驱动：以 orderItemId 匹配，新增/修改/删除各留痕）
     * items 在 Map 中以 List<Map<String, Object>> 形式传递；
     * 留痕字段列表从 fieldConfig 的 14.3 subFields 中读取，不再硬编码字段名
     */
    @SuppressWarnings("unchecked")
    private void processItemModification(OrderMainEntity order, Map<String, Object> modifications,
            Long applyId, Long modifierId, String modifierName, ModifyApplyFieldConfigDTO fieldConfig) {
        Object itemsObj = modifications.get("items");
        List<Map<String, Object>> newItems = Convert.convert(List.class, itemsObj);

        // 0. newItems 为 null 时等同于空列表（删除所有旧项目）
        if (newItems == null) {
            newItems = List.of();
        }

        // 提前校验：不允许将所有明细删除
        if (newItems.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ITEM_REQUIRED);
        }

        // 读取 14.3 配置的 item 子字段列表（用于留痕对比）
        ModifyApplyFieldConfigDTO.TypeConfig itemTypeConfig =
                fieldConfig.getTypeConfig(ModifyApplyTypeEnum.ITEM.getDictCode());
        List<ModifyApplyFieldConfigDTO.FieldConfig> itemSubFields =
                itemTypeConfig != null ? itemTypeConfig.getItemSubFields() : List.of();

        // 1. 查询旧 items
        List<OrderItemEntity> oldItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, order.getId())
                        .orderByAsc(OrderItemEntity::getSortOrder)
        );

        // 2. 校验所有提供的 orderItemId 确实属于当前订单
        Set<Long> validItemIds = oldItems.stream()
                .map(OrderItemEntity::getId)
                .collect(Collectors.toSet());
        for (Map<String, Object> item : newItems) {
            Long orderItemId = Convert.convert(Long.class, item.get("orderItemId"));
            if (orderItemId != null && !validItemIds.contains(orderItemId)) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "重建项目ID不属于当前订单");
            }
        }

        // 3. 以 orderItemId 分组：准备旧记录 Map（已处理项从 Map 中移除，剩余为删除项）
        Map<Long, OrderItemEntity> oldItemMap = oldItems.stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        int sortOrder = 1;
        for (Map<String, Object> itemMap : newItems) {
            Long orderItemId = Convert.convert(Long.class, itemMap.get("orderItemId"));
            if (orderItemId != null && oldItemMap.containsKey(orderItemId)) {
                // 修改：快照前值 → 更新实体 → 快照后值 → 对比留痕
                OrderItemEntity oldItem = oldItemMap.remove(orderItemId);
                List<String> subFieldNames = itemSubFields.stream()
                        .map(ModifyApplyFieldConfigDTO.FieldConfig::getField).toList();
                Map<String, Object> beforeSnapshot = snapshotFields(oldItem, subFieldNames);
                // 从 Map 复制到实体（排除 orderItemId）
                BeanUtil.copyProperties(itemMap, oldItem, "orderItemId");
                oldItem.setSortOrder(sortOrder++);
                // 重新校验并填充冗余字段（bodyPartName、projectName 等）
                orderDataValidator.validateAndFillItemsForOrder(
                        List.of(oldItem), OrderDataValidator.ValidateMode.DIRECT);
                Map<String, Object> afterSnapshot = snapshotFields(oldItem, subFieldNames);
                // 对比快照生成留痕（key 加 item 维度前缀区分多项目场景）
                for (ModifyApplyFieldConfigDTO.FieldConfig fc : itemSubFields) {
                    String fn = fc.getField();
                    Object oldVal = beforeSnapshot.get(fn);
                    Object newVal = afterSnapshot.get(fn);
                    if (newVal != null && !Objects.equals(oldVal, newVal)) {
                        String label = StrUtil.blankToDefault(fc.getLabel(), fn);
                        recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                                "item_" + oldItem.getId() + "_" + fn, label,
                                oldVal, newVal, modifierId, modifierName);
                    }
                }
                orderItemMapper.updateById(oldItem);
            } else {
                // 新增：校验插入
                OrderItemEntity newEntity = new OrderItemEntity();
                BeanUtil.copyProperties(itemMap, newEntity);
                newEntity.setOrderId(order.getId());
                newEntity.setOrderCode(order.getOrderCode());
                newEntity.setSortOrder(sortOrder++);
                orderDataValidator.validateAndFillItemsForOrder(
                        List.of(newEntity), OrderDataValidator.ValidateMode.DIRECT);
                orderItemMapper.insert(newEntity);
                // 记录新增留痕（projectName 可能未填充，使用 projectDesc 兜底）
                String newItemDesc = newEntity.getProjectName() != null
                        ? newEntity.getProjectName() : Convert.convert(String.class, itemMap.get("projectDesc"));
                recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                        "item_new", "新增重建项目",
                        null, newItemDesc != null ? newItemDesc : "新项目", modifierId, modifierName);
            }
        }

        // 删除：oldItemMap 中剩余为未出现在 newItems 中的旧项目
        for (OrderItemEntity deletedItem : oldItemMap.values()) {
            orderItemMapper.deleteById(deletedItem.getId());
            recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                    "item_" + deletedItem.getId(), "删除重建项目",
                    deletedItem.getProjectName(), null, modifierId, modifierName);
        }
    }

    // ==================== 辅助方法：影像文件修改 ====================

    /**
     * 处理影像文件修改（先校验文件存在性，再按类别替换）
     */
    @SuppressWarnings("unchecked")
    private void processImageModification(OrderMainEntity order, Map<String, Object> modifications,
            Long applyId, Long modifierId, String modifierName) {
        List<String> imageDataFileIds = Convert.convert(List.class, modifications.get("imageDataFileIds"));
        List<String> imageReportFileIds = Convert.convert(List.class, modifications.get("imageReportFileIds"));

        if (imageDataFileIds != null) {
            validateFileIds(imageDataFileIds, "影像数据");
            replaceOrderFiles(order, imageDataFileIds,
                    FileBizTypeEnum.IMAGE_DATA.getDictCode(), applyId, modifierId, modifierName);
        }
        if (imageReportFileIds != null) {
            validateFileIds(imageReportFileIds, "影像报告");
            replaceOrderFiles(order, imageReportFileIds,
                    FileBizTypeEnum.IMAGE_REPORT.getDictCode(), applyId, modifierId, modifierName);
        }
    }

    /**
     * 校验文件ID列表中的文件均存在于 file_detail 表
     * 使用 Set 去重后比较，防止客户端传入重复 ID 导致误判；同时限制单次最大数量防止 DoS
     */
    private void validateFileIds(List<String> fileIds, String fileTypeName) {
        if (CollUtil.isEmpty(fileIds)) return;
        if (fileIds.size() > 50) {
            throw new BusinessException(400, fileTypeName + "文件数量不能超过50个");
        }
        Set<String> inputSet = new HashSet<>(fileIds);
        List<FileVO> found = fileService.listByIds(new ArrayList<>(inputSet));
        Set<String> foundSet = found.stream().map(FileVO::getId).collect(Collectors.toSet());
        if (!foundSet.containsAll(inputSet)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_FILE_NOT_FOUND, fileTypeName + "文件");
        }
    }

    /**
     * 替换订单文件关联（按 fileCategory 逻辑删除旧关联，插入新关联，记录留痕）
     */
    private void replaceOrderFiles(OrderMainEntity order, List<String> newFileIds,
            String fileCategory, Long applyId, Long modifierId, String modifierName) {
        // 查询旧文件关联
        List<OrderFileEntity> oldFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, order.getId())
                        .eq(OrderFileEntity::getFileCategory, fileCategory)
        );

        // 逻辑删除旧文件关联（MyBatis-Plus @TableLogic 自动将 is_deleted 置为 1）
        for (OrderFileEntity oldFile : oldFiles) {
            orderFileMapper.deleteById(oldFile.getId());
        }

        // 插入新文件关联
        for (String fileId : newFileIds) {
            OrderFileEntity newFile = new OrderFileEntity();
            newFile.setOrderId(order.getId());
            newFile.setOrderCode(order.getOrderCode());
            newFile.setFileId(fileId);
            newFile.setFileCategory(fileCategory);
            orderFileMapper.insert(newFile);
        }

        // 记录留痕（JSON 数组格式记录前后文件 ID 列表）
        List<String> oldFileIds = oldFiles.stream().map(OrderFileEntity::getFileId).toList();
        recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                fileCategory, fileCategory,
                JSONUtil.toJsonStr(oldFileIds),
                JSONUtil.toJsonStr(newFileIds),
                modifierId, modifierName);
    }

    // ==================== 查询方法 ====================

    /**
     * 查询当前用户发起的申请列表（分页）
     *
     * @param dto 查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModifyApplyListVO> listMyApplies(ModifyApplyPageQueryDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        log.info("查询我的修改申请列表，userId={}, pageNum={}", currentUserId, dto.getPageNum());
        Page<OrderModifyApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModifyApplyEntity> wrapper = new LambdaQueryWrapper<OrderModifyApplyEntity>()
                .eq(OrderModifyApplyEntity::getApplicantId, currentUserId)
                .eq(StrUtil.isNotBlank(dto.getStatus()), OrderModifyApplyEntity::getStatus, dto.getStatus())
                .like(StrUtil.isNotBlank(dto.getOrderCode()), OrderModifyApplyEntity::getOrderCode, dto.getOrderCode())
                .orderByDesc(OrderModifyApplyEntity::getCreateTime)
                .orderByDesc(OrderModifyApplyEntity::getId);
        IPage<OrderModifyApplyEntity> entityPage = orderModifyApplyMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toListVO);
    }

    /**
     * 查询待审核申请列表（管理员）
     *
     * @param dto 查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModifyApplyListVO> listPendingApplies(ModifyApplyPageQueryDTO dto) {
        log.info("查询待审核修改申请列表，pageNum={}", dto.getPageNum());
        Page<OrderModifyApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModifyApplyEntity> wrapper = new LambdaQueryWrapper<OrderModifyApplyEntity>()
                .eq(OrderModifyApplyEntity::getStatus, ModifyApplyStatusEnum.PENDING.getCode())
                .like(StrUtil.isNotBlank(dto.getOrderCode()), OrderModifyApplyEntity::getOrderCode, dto.getOrderCode())
                .like(StrUtil.isNotBlank(dto.getApplicantName()),
                        OrderModifyApplyEntity::getApplicantName, dto.getApplicantName())
                .orderByAsc(OrderModifyApplyEntity::getCreateTime)
                .orderByDesc(OrderModifyApplyEntity::getId);
        IPage<OrderModifyApplyEntity> entityPage = orderModifyApplyMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toListVO);
    }

    /**
     * 查询申请详情
     *
     * @param applyId 申请ID
     * @return 申请详情 VO
     */
    @Override
    public ModifyApplyDetailVO getApplyDetail(Long applyId) {
        log.info("查询修改申请详情，applyId={}", applyId);
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        ModifyApplyDetailVO vo = new ModifyApplyDetailVO();
        BeanUtils.copyProperties(apply, vo);
        // 填充状态文本
        ModifyApplyStatusEnum statusEnum = ModifyApplyStatusEnum.getByCode(apply.getStatus());
        if (statusEnum != null) {
            vo.setStatusText(statusEnum.getName());
        }
        // 填充订单冗余信息（医院名、阶段等）
        OrderMainEntity order = orderMainMapper.selectById(apply.getOrderId());
        if (order != null) {
            vo.setHospitalName(order.getHospitalName());
            vo.setPatientName(order.getPatientName());
            vo.setDoctorName(order.getDoctorName());
            vo.setCurrentPhase(order.getPhase());
            vo.setCurrentStatus(order.getStatus());
            // 填充阶段和状态中文名
            FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(order.getPhase());
            if (phaseEnum != null) {
                vo.setCurrentPhaseText(phaseEnum.getName());
            }
            FlowStatusEnum statusByValue = FlowStatusEnum.getByValue(order.getStatus());
            if (statusByValue != null) {
                vo.setCurrentStatusText(statusByValue.getName());
            }
        }
        return vo;
    }

    /**
     * 查询订单的所有申请记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModifyApplyListVO> listAppliesByOrder(Long orderId, ModifyApplyPageQueryDTO dto) {
        log.info("查询订单的修改申请记录，orderId={}", orderId);
        Page<OrderModifyApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModifyApplyEntity> wrapper = new LambdaQueryWrapper<OrderModifyApplyEntity>()
                .eq(OrderModifyApplyEntity::getOrderId, orderId)
                .eq(StrUtil.isNotBlank(dto.getStatus()), OrderModifyApplyEntity::getStatus, dto.getStatus())
                .orderByDesc(OrderModifyApplyEntity::getCreateTime);
        IPage<OrderModifyApplyEntity> entityPage = orderModifyApplyMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toListVO);
    }

    /**
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto) {
        log.info("查询修改留痕记录，orderId={}", orderId);
        Page<OrderModificationLogEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModificationLogEntity> wrapper =
                new LambdaQueryWrapper<OrderModificationLogEntity>()
                        .eq(OrderModificationLogEntity::getOrderId, orderId)
                        .eq(StrUtil.isNotBlank(dto.getFieldName()),
                                OrderModificationLogEntity::getFieldName, dto.getFieldName())
                        .orderByDesc(OrderModificationLogEntity::getCreateTime);
        IPage<OrderModificationLogEntity> entityPage = orderModificationLogMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toLogVO);
    }

    /**
     * 校验字段是否在申请允许的修改范围内
     * 供设计模块等复用
     *
     * @param applyId    申请ID
     * @param fieldNames 要修改的字段名列表
     */
    @Override
    public void validateFieldsInScope(Long applyId, List<String> fieldNames) {
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        if (!ModifyApplyStatusEnum.APPROVED.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR);
        }
        Set<String> allowedTypes = Arrays.stream(apply.getApplyTypeCodes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        // 加载字段配置，校验字段是否在申请范围内
        ModifyApplyFieldConfigDTO config = loadFieldConfig();
        for (String fieldName : fieldNames) {
            if (!isFieldInScope(fieldName, allowedTypes, config)) {
                log.warn("字段不在申请范围内，applyId={}, fieldName={}", applyId, fieldName);
                throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED);
            }
        }
    }

    /**
     * 判断字段是否在允许类型的配置范围内
     */
    private boolean isFieldInScope(String fieldName, Set<String> allowedTypes,
            ModifyApplyFieldConfigDTO config) {
        // 逐类型检查字段是否在允许的字段列表内
        for (String typeCode : allowedTypes) {
            ModifyApplyFieldConfigDTO.TypeConfig typeConfig = config.getTypeConfig(typeCode);
            if (typeConfig != null && typeConfig.containsField(fieldName)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 通用辅助方法 ====================

    /**
     * 加载字段配置（带兜底默认配置）
     * <p>
     * 使用 Jackson ObjectMapper 解析，保证 @JsonAnySetter 注解生效，
     * 将 JSON 顶层动态 key（如 "14.1"）正确映射到 typeConfigs Map 中
     */
    private ModifyApplyFieldConfigDTO loadFieldConfig() {
        String configJson = configService.getConfigValue(
                SystemConfigKeyEnum.ORDER_MODIFY_FIELD_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("订单修改字段配置为空，使用默认配置");
            return new ModifyApplyFieldConfigDTO();
        }
        try {
            return objectMapper.readValue(configJson, ModifyApplyFieldConfigDTO.class);
        } catch (Exception e) {
            log.error("解析订单修改字段配置异常，使用默认配置", e);
            return new ModifyApplyFieldConfigDTO();
        }
    }

    /**
     * 记录修改留痕
     */
    private void recordModificationLog(Long orderId, String orderCode, Long applyId,
            String fieldName, String fieldLabel, Object oldValue, Object newValue,
            Long modifierId, String modifierName) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        OrderModificationLogEntity logEntity = new OrderModificationLogEntity();
        logEntity.setOrderId(orderId);
        logEntity.setOrderCode(orderCode);
        logEntity.setApplyId(applyId);
        logEntity.setFieldName(fieldName);
        logEntity.setFieldLabel(fieldLabel);
        logEntity.setOldValue(oldValue != null ? oldValue.toString() : null);
        logEntity.setNewValue(newValue != null ? newValue.toString() : null);
        logEntity.setModifierId(modifierId);
        logEntity.setModifierName(modifierName);
        orderModificationLogMapper.insert(logEntity);
    }

    /**
     * 获取当前登录用户姓名
     */
    private String getCurrentUserName() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            UserEntity user = userService.getById(userId);
            return user != null ? user.getRealName() : null;
        } catch (Exception e) {
            log.warn("获取当前用户姓名失败", e);
            return null;
        }
    }

    // ==================== Entity → VO 转换 ====================

    private ModifyApplyVO toApplyVO(OrderModifyApplyEntity apply) {
        ModifyApplyVO vo = new ModifyApplyVO();
        BeanUtils.copyProperties(apply, vo);
        ModifyApplyStatusEnum statusEnum = ModifyApplyStatusEnum.getByCode(apply.getStatus());
        if (statusEnum != null) {
            vo.setStatusText(statusEnum.getName());
        }
        return vo;
    }

    private ModifyApplyListVO toListVO(OrderModifyApplyEntity apply) {
        ModifyApplyListVO vo = new ModifyApplyListVO();
        BeanUtils.copyProperties(apply, vo);
        ModifyApplyStatusEnum statusEnum = ModifyApplyStatusEnum.getByCode(apply.getStatus());
        if (statusEnum != null) {
            vo.setStatusText(statusEnum.getName());
        }
        return vo;
    }

    private ModificationLogVO toLogVO(OrderModificationLogEntity entity) {
        ModificationLogVO vo = new ModificationLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    // ==================== 列表角标填充 ====================

    /**
     * 批量填充订单列表的修改申请角标信息
     * <p>
     * 一次性查询所有订单的有效申请（PENDING/APPROVED），按订单分组后批量填充，避免 N+1 查询。
     * 每个订单只保留最近一条有效申请（优先 PENDING，次选 APPROVED）。
     *
     * @param voList 订单列表 VO
     */
    @Override
    public void fillModifyApplyStatus(List<OrderListVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        // 提取所有订单ID
        List<Long> orderIds = voList.stream()
                .map(OrderListVO::getId)
                .collect(Collectors.toList());
        // 一次性查询所有有效申请
        List<OrderModifyApplyEntity> activeApplies = orderModifyApplyMapper.selectList(
                new LambdaQueryWrapper<OrderModifyApplyEntity>()
                        .in(OrderModifyApplyEntity::getOrderId, orderIds)
                        .in(OrderModifyApplyEntity::getStatus,
                                ModifyApplyStatusEnum.PENDING.getCode(),
                                ModifyApplyStatusEnum.APPROVED.getCode())
        );
        if (activeApplies.isEmpty()) {
            return;
        }
        // 按 orderId 分组（每个订单只取最关键的一条：PENDING 优先于 APPROVED）
        Map<Long, OrderModifyApplyEntity> applyByOrderId = activeApplies.stream()
                .collect(Collectors.toMap(
                        OrderModifyApplyEntity::getOrderId,
                        apply -> apply,
                        (a, b) -> {
                            // PENDING 优先
                            if (ModifyApplyStatusEnum.PENDING.getCode().equals(a.getStatus())) {
                                return a;
                            }
                            if (ModifyApplyStatusEnum.PENDING.getCode().equals(b.getStatus())) {
                                return b;
                            }
                            // 两条均为 APPROVED，正常不应发生（业务约束：同一订单最多一条有效申请）
                            log.warn("订单存在多条 APPROVED 申请，取 ID 最大（最新）的一条，orderId={}", a.getOrderId());
                            return a.getId() > b.getId() ? a : b;
                        }
                ));
        // 批量填充角标信息
        for (OrderListVO vo : voList) {
            OrderModifyApplyEntity apply = applyByOrderId.get(vo.getId());
            if (apply != null) {
                vo.setPendingModifyApplyStatus(apply.getStatus());
                vo.setPendingModifyApplyId(apply.getId());
            }
        }
    }

    /**
     * 校验订单是否存在阻断主流程的修改申请（PENDING 或 APPROVED 状态）
     * PENDING 优先检查（申请等待审核时，流程不应推进）
     * APPROVED 次之（申请已批准但未执行时，流程不应推进，防止修改内容丢失）
     *
     * @param orderId 订单ID
     */
    @Override
    public void validateNoBlockingModifyApply(Long orderId) {
        // 一次查询捞出所有阻断状态的申请，通过流式判断减少 DB 交互
        List<OrderModifyApplyEntity> blockingApplies = orderModifyApplyMapper.selectList(
                new LambdaQueryWrapper<OrderModifyApplyEntity>()
                        .eq(OrderModifyApplyEntity::getOrderId, orderId)
                        .in(OrderModifyApplyEntity::getStatus,
                                ModifyApplyStatusEnum.PENDING.getCode(),
                                ModifyApplyStatusEnum.APPROVED.getCode())
        );
        if (CollUtil.isEmpty(blockingApplies)) {
            return;
        }
        // PENDING 优先：有待审核申请时立即阻断
        boolean hasPending = blockingApplies.stream()
                .anyMatch(a -> ModifyApplyStatusEnum.PENDING.getCode().equals(a.getStatus()));
        if (hasPending) {
            log.warn("订单存在待审核的修改申请，拒绝流转，orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_HAS_PENDING_MODIFY_APPLY);
        }
        // 有已批准但未执行的申请时阻断（取第一条 APPROVED 申请的 ID 供排查）
        OrderModifyApplyEntity approvedApply = blockingApplies.stream()
                .filter(a -> ModifyApplyStatusEnum.APPROVED.getCode().equals(a.getStatus()))
                .findFirst().orElse(null);
        log.warn("订单存在已批准但未执行的修改申请，拒绝流转，orderId={}, applyId={}",
                orderId, approvedApply != null ? approvedApply.getId() : null);
        throw new BusinessException(ErrorCodeEnum.ORDER_HAS_APPROVED_MODIFY_APPLY);
    }
}
