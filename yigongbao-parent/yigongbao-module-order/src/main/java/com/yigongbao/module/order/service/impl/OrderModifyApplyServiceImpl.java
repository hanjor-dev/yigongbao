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
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.dto.modify.AuditModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.CreateModifyApplyDTO;
import com.yigongbao.module.order.dto.modify.ExecuteModificationDTO;
import com.yigongbao.module.order.dto.modify.ExecuteModificationItemDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyFieldConfigDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

        // 校验订单阶段（仅 phase=1 或 phase=2 可申请）
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

        // 2. 校验订单阶段适用修改申请（phase=1 或 phase=2）
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
        orderModifyApplyMapper.insert(apply);

        log.info("发起修改申请成功，applyId={}", apply.getId());
        return toApplyVO(apply);
    }

    /**
     * 校验申请类型有效性（枚举校验，无需 DB 查询）
     */
    private void validateApplyTypes(String applyTypes) {
        if (StrUtil.isBlank(applyTypes)) {
            throw new BusinessException(400, "申请类型不能为空");
        }
        for (String type : applyTypes.split(",")) {
            if (ModifyApplyTypeEnum.getByDictCode(type.trim()) == null) {
                throw new BusinessException(400, "存在无效的申请类型：" + type.trim());
            }
        }
    }

    /**
     * 校验申请类型是否在当前阶段允许
     * 订单阶段（phase=1）：允许全部类型；设计阶段（phase=2）：仅允许 14.3（重建项目）
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

        // 校验审核操作有效性
        String action = dto.getAction().toUpperCase();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BusinessException(400, "审核操作无效：" + dto.getAction());
        }
        if ("REJECT".equals(action) && StrUtil.isBlank(dto.getRejectReason())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_REJECT_REASON_REQUIRED);
        }

        // 更新申请状态
        Long auditorId = StpUtil.getLoginIdAsLong();
        String auditorName = getCurrentUserName();
        apply.setAuditorId(auditorId);
        apply.setAuditorName(auditorName);
        apply.setAuditTime(LocalDateTime.now());
        if ("APPROVE".equals(action)) {
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
     * @param orderId 订单ID
     * @param applyId 修改申请ID
     * @param dto     修改字段参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeModification(Long orderId, Long applyId, ExecuteModificationDTO dto) {
        log.info("执行订单修改，orderId={}, applyId={}", orderId, applyId);
        // 1. 校验申请存在且状态为 APPROVED
        OrderModifyApplyEntity apply = orderModifyApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_FOUND);
        }
        if (!ModifyApplyStatusEnum.APPROVED.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR);
        }
        // 2. 校验申请与订单关联正确
        if (!apply.getOrderId().equals(orderId)) {
            throw new BusinessException(400, "订单与申请不匹配");
        }

        // 3. 解析允许的申请类型（trim 防止存储时残留空格）
        Set<String> allowedTypes = Arrays.stream(apply.getApplyTypeCodes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        // 4. 查询订单实体
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 5. 获取当前操作人
        Long modifierId = StpUtil.getLoginIdAsLong();
        String modifierName = getCurrentUserName();

        // 6. 处理基础信息修改（14.1）
        if (allowedTypes.contains(ModifyApplyTypeEnum.INFO.getDictCode())) {
            processInfoModification(order, dto, applyId, modifierId, modifierName);
        }

        // 7. 处理重建项目修改（14.3）
        if (allowedTypes.contains(ModifyApplyTypeEnum.ITEM.getDictCode()) && dto.getItems() != null) {
            processItemModification(order, dto, applyId, modifierId, modifierName);
        }

        // 8. 处理影像文件修改（14.2）
        if (allowedTypes.contains(ModifyApplyTypeEnum.IMAGE.getDictCode())) {
            processImageModification(order, dto, applyId, modifierId, modifierName);
        }

        log.info("执行订单修改成功，orderId={}, applyId={}", orderId, applyId);

        // 9. 将申请状态置为 COMPLETED（防止重复执行）
        apply.setStatus(ModifyApplyStatusEnum.COMPLETED.getCode());
        orderModifyApplyMapper.updateById(apply);
    }

    // ==================== 辅助方法：基础信息修改 ====================

    /**
     * 处理基础信息修改（显式字段赋值 + 显式留痕，无反射）
     */
    private void processInfoModification(OrderMainEntity order, ExecuteModificationDTO dto,
            Long applyId, Long modifierId, String modifierName) {
        // 1. 保存修改前快照（各字段的当前值）
        Long oldHospitalId                = order.getHospitalId();
        String oldPatientName             = order.getPatientName();
        Integer oldPatientAge             = order.getPatientAge();
        String oldPatientGender           = order.getPatientGender();
        Long oldDoctorId                  = order.getDoctorId();
        String oldDoctorPhone             = order.getDoctorPhone();
        Integer oldIsUrgent               = order.getIsUrgent();
        Integer oldIsPostal               = order.getIsPostal();
        String oldPostalAddress           = order.getPostalAddress();
        LocalDateTime oldExpectedDate     = order.getExpectedDeliveryDate();

        // 2. 更新订单字段（含医院/科室/医生冗余字段同步、quickAdd）
        applyInfoFields(order, dto);
        orderMainMapper.updateById(order);

        // 3. 逐字段对比记录留痕
        // 注意：doctorId 用 order.getDoctorId() 作为新值，因为 quickAdd 场景下 dto.getDoctorId() 为 null
        //       而实际写入的是 quickAdd 返回的 doctor.id
        logInfoFieldChanges(order.getId(), order.getOrderCode(), applyId,
                oldHospitalId, oldPatientName, oldPatientAge, oldPatientGender,
                oldDoctorId, oldDoctorPhone, oldIsUrgent, oldIsPostal, oldPostalAddress, oldExpectedDate,
                order, dto, modifierId, modifierName);
    }

    /**
     * 将 dto 中非 null 的字段赋值到订单实体（显式赋值，无反射）
     * 医院/科室/医生走 OrderDataValidator 统一校验+填充路径，保证冗余字段同步
     */
    private void applyInfoFields(OrderMainEntity order, ExecuteModificationDTO dto) {
        // 医院/科室/医生统一走 validator（含冗余字段同步、存在性校验、quickAdd）
        boolean hasDoctorChange = dto.getDoctorId() != null || StrUtil.isNotBlank(dto.getDoctorName());
        if (dto.getHospitalId() != null || dto.getHospitalDeptId() != null || hasDoctorChange) {
            orderDataValidator.validateAndFillForModify(order,
                    dto.getHospitalId(), dto.getHospitalDeptId(),
                    dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone());
        }
        // 其他基础字段（不涉及冗余同步，直接赋值）
        if (dto.getPatientName()          != null) order.setPatientName(dto.getPatientName());
        if (dto.getPatientAge()           != null) order.setPatientAge(dto.getPatientAge());
        if (dto.getPatientGender()        != null) order.setPatientGender(dto.getPatientGender());
        if (dto.getIsUrgent()             != null) order.setIsUrgent(dto.getIsUrgent());
        if (dto.getIsPostal()             != null) order.setIsPostal(dto.getIsPostal());
        if (dto.getPostalAddress()        != null) order.setPostalAddress(dto.getPostalAddress());
        if (dto.getExpectedDeliveryDate() != null) order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
    }

    /**
     * 逐字段对比并记录留痕（显式比对，无反射）
     * doctorId / doctorPhone 取修改后的 order 值，以覆盖 quickAdd 场景（dto.getDoctorId() 此时为 null）
     */
    private void logInfoFieldChanges(Long orderId, String orderCode, Long applyId,
            Long oldHospitalId, String oldPatientName, Integer oldPatientAge, String oldPatientGender,
            Long oldDoctorId, String oldDoctorPhone, Integer oldIsUrgent,
            Integer oldIsPostal, String oldPostalAddress, LocalDateTime oldExpectedDate,
            OrderMainEntity order, ExecuteModificationDTO dto, Long modifierId, String modifierName) {
        recordIfChanged(orderId, orderCode, applyId, "hospitalId", "医院",
                oldHospitalId, dto.getHospitalId(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "patientName", "患者姓名",
                oldPatientName, dto.getPatientName(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "patientAge", "患者年龄",
                oldPatientAge, dto.getPatientAge(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "patientGender", "患者性别",
                oldPatientGender, dto.getPatientGender(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "doctorId", "关联医生",
                oldDoctorId, order.getDoctorId(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "doctorPhone", "医生电话",
                oldDoctorPhone, order.getDoctorPhone(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "isUrgent", "是否加急",
                oldIsUrgent, dto.getIsUrgent(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "isPostal", "是否邮寄",
                oldIsPostal, dto.getIsPostal(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "postalAddress", "邮寄地址",
                oldPostalAddress, dto.getPostalAddress(), modifierId, modifierName);
        recordIfChanged(orderId, orderCode, applyId, "expectedDeliveryDate", "期望交付时间",
                oldExpectedDate, dto.getExpectedDeliveryDate(), modifierId, modifierName);
    }

    /**
     * 仅在值发生变化时记录留痕
     */
    private void recordIfChanged(Long orderId, String orderCode, Long applyId,
            String fieldName, String fieldLabel, Object oldValue, Object newValue,
            Long modifierId, String modifierName) {
        if (newValue != null && !Objects.equals(oldValue, newValue)) {
            recordModificationLog(orderId, orderCode, applyId,
                    fieldName, fieldLabel, oldValue, newValue, modifierId, modifierName);
        }
    }

    // ==================== 辅助方法：重建项目修改 ====================

    /**
     * 处理重建项目修改（增量差异：以 orderItemId 匹配，新增/修改/删除各留痕）
     */
    private void processItemModification(OrderMainEntity order, ExecuteModificationDTO dto,
            Long applyId, Long modifierId, String modifierName) {
        // 0. 重建订单的重建项目不允许为空
        if (CollUtil.isEmpty(dto.getItems())) {
            throw new BusinessException(400, "重建项目不能为空");
        }

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
        for (ExecuteModificationItemDTO item : dto.getItems()) {
            if (item.getOrderItemId() != null && !validItemIds.contains(item.getOrderItemId())) {
                throw new BusinessException(400, "重建项目ID不属于当前订单：" + item.getOrderItemId());
            }
        }

        // 3. 以 orderItemId 分组：准备旧记录 Map（已处理项从 Map 中移除，剩余为删除项）
        Map<Long, OrderItemEntity> oldItemMap = oldItems.stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        int sortOrder = 1;
        for (ExecuteModificationItemDTO newItem : dto.getItems()) {
            if (newItem.getOrderItemId() != null && oldItemMap.containsKey(newItem.getOrderItemId())) {
                // 修改：逐字段对比留痕，然后更新
                OrderItemEntity oldItem = oldItemMap.remove(newItem.getOrderItemId());
                compareAndLogItemFields(order, oldItem, newItem, applyId, modifierId, modifierName);
                BeanUtils.copyProperties(newItem, oldItem, "orderItemId");
                oldItem.setSortOrder(sortOrder++);
                // 重新校验并填充冗余字段（bodyPartName、projectName 等）
                orderDataValidator.validateAndFillItemsForOrder(
                        List.of(oldItem), OrderDataValidator.ValidateMode.DIRECT);
                orderItemMapper.updateById(oldItem);
            } else {
                // 新增：校验插入
                OrderItemEntity newEntity = new OrderItemEntity();
                BeanUtils.copyProperties(newItem, newEntity);
                newEntity.setOrderId(order.getId());
                newEntity.setOrderCode(order.getOrderCode());
                newEntity.setSortOrder(sortOrder++);
                orderDataValidator.validateAndFillItemsForOrder(
                        List.of(newEntity), OrderDataValidator.ValidateMode.DIRECT);
                orderItemMapper.insert(newEntity);
                // 记录新增留痕（projectName 可能未填充，使用 projectDesc 兜底）
                String newItemDesc = newEntity.getProjectName() != null
                        ? newEntity.getProjectName() : newItem.getProjectDesc();
                recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                        "item_new", "新增重建项目",
                        null, newItemDesc != null ? newItemDesc : "新项目", modifierId, modifierName);
            }
        }

        // 删除：oldItemMap 中剩余为未出现在 dto.items 中的旧项目
        for (OrderItemEntity deletedItem : oldItemMap.values()) {
            orderItemMapper.deleteById(deletedItem.getId());
            recordModificationLog(order.getId(), order.getOrderCode(), applyId,
                    "item_" + deletedItem.getId(), "删除重建项目",
                    deletedItem.getProjectName(), null, modifierId, modifierName);
        }
    }

    /**
     * 对比重建项目字段变更并记录留痕
     */
    private void compareAndLogItemFields(OrderMainEntity order, OrderItemEntity oldItem,
            ExecuteModificationItemDTO newItem, Long applyId, Long modifierId, String modifierName) {
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_bodyPartId", "部位",
                oldItem.getBodyPartId(), newItem.getBodyPartId(), modifierId, modifierName);
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_projectId", "重建项目",
                oldItem.getProjectId(), newItem.getProjectId(), modifierId, modifierName);
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_projectDesc", "项目说明",
                oldItem.getProjectDesc(), newItem.getProjectDesc(), modifierId, modifierName);
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_formingRequirement", "成形需求",
                oldItem.getFormingRequirement(), newItem.getFormingRequirement(), modifierId, modifierName);
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_otherRequirement", "其他要求",
                oldItem.getOtherRequirement(), newItem.getOtherRequirement(), modifierId, modifierName);
    }

    // ==================== 辅助方法：影像文件修改 ====================

    /**
     * 处理影像文件修改（先校验文件存在性，再按类别替换）
     */
    private void processImageModification(OrderMainEntity order, ExecuteModificationDTO dto,
            Long applyId, Long modifierId, String modifierName) {
        if (dto.getImageDataFileIds() != null) {
            validateFileIds(dto.getImageDataFileIds(), "影像数据");
            replaceOrderFiles(order, dto.getImageDataFileIds(),
                    FileBizTypeEnum.IMAGE_DATA.getDictCode(), applyId, modifierId, modifierName);
        }
        if (dto.getImageReportFileIds() != null) {
            validateFileIds(dto.getImageReportFileIds(), "影像报告");
            replaceOrderFiles(order, dto.getImageReportFileIds(),
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
                .orderByDesc(OrderModifyApplyEntity::getCreateTime);
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
                .orderByAsc(OrderModifyApplyEntity::getCreateTime);
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
        // 填充订单冗余信息（医院名、科室、阶段等）
        OrderMainEntity order = orderMainMapper.selectById(apply.getOrderId());
        if (order != null) {
            vo.setHospitalName(order.getHospitalName());
            vo.setHospitalDeptName(order.getHospitalDeptName());
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
     */
    private ModifyApplyFieldConfigDTO loadFieldConfig() {
        String configJson = configService.getConfigValue(
                SystemConfigKeyEnum.ORDER_MODIFY_FIELD_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("订单修改字段配置为空，使用默认配置");
            return new ModifyApplyFieldConfigDTO();
        }
        try {
            return JSONUtil.toBean(configJson, ModifyApplyFieldConfigDTO.class);
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
}
