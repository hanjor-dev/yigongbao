package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.PhysicalDeliveryConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.order.constant.OrderModifyObjectType;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO;
import com.yigongbao.module.order.dto.modify.ObjectChange;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.service.OrderModifyFullService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.validator.OrderDataScopeChecker;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * 订单全量修改 Service 实现类
 * 前端传入完整订单数据，后端自动判断变更内容
 *
 * @author hanjor
 * @date 2026-05-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderModifyFullServiceImpl implements OrderModifyFullService {

    private static final int MAX_FIELD_VALUE_LENGTH = 20;
    private static final Set<String> ADMIN_ROLES = Set.of(RoleCodeEnum.ADMIN.getCode(), RoleCodeEnum.COMPANY_ADMIN.getCode());
    private static final Set<String> DESIGNER_ROLES = Set.of(RoleCodeEnum.DESIGNER.getCode(), RoleCodeEnum.DESIGNER_MANAGER.getCode());
    private static final Set<String> BUSINESS_ROLES = Set.of(RoleCodeEnum.SALESMAN.getCode(), RoleCodeEnum.SALESMAN_SELF.getCode());

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderFileMapper orderFileMapper;
    private final OrderModificationLogMapper orderModificationLogMapper;
    private final OrderDataValidator orderDataValidator;
    private final OrderDataScopeChecker orderDataScopeChecker;
    private final FlowFacade flowFacade;
    private final OrgService orgService;
    private final HospitalDeptService hospitalDeptService;
    private final UserService userService;
    private final com.yigongbao.module.system.dict.service.DictService dictService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyOrderFull(Long orderId, OrderModifyFullDTO dto) {
        modifyOrderFull(orderId, dto, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyOrderFull(Long orderId, OrderModifyFullDTO dto, boolean skipPermissionCheck) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userService.getById(userId);
        String userName = currentUser != null ? currentUser.getRealName() : null;
        String roleCode = currentUser != null ? currentUser.getRoleCode() : "";
        modifyOrderFull(orderId, dto, skipPermissionCheck, userId, userName, roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyOrderFull(Long orderId, OrderModifyFullDTO dto, boolean skipPermissionCheck, Long modifierId, String modifierName, String modifierRoleCode) {
        modifyOrderFull(orderId, dto, skipPermissionCheck, modifierId, modifierName, modifierRoleCode, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyOrderFull(Long orderId, OrderModifyFullDTO dto, boolean skipPermissionCheck,
                                Long modifierId, String modifierName, String modifierRoleCode, Long applyId) {
        if (!skipPermissionCheck) {
            orderDataScopeChecker.checkOrderAccess(orderId);
        }
        // 1. 查询当前订单
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "订单不存在");
        }
        validatePhysicalDeliveryValue(dto.getNeedsPhysicalDelivery());
        validatePhysicalDeliveryChange(order, dto.getNeedsPhysicalDelivery());

        // 2. 获取当前用户角色（用于权限校验）
        Long currentUserId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userService.getById(currentUserId);
        String currentRoleCode = currentUser != null ? currentUser.getRoleCode() : "";

        boolean isAdmin = ADMIN_ROLES.contains(currentRoleCode);
        boolean isBusinessRole = BUSINESS_ROLES.contains(currentRoleCode);
        boolean isDesigner = DESIGNER_ROLES.contains(currentRoleCode);
        int phase = order.getPhase();
        boolean isDesignPhase = phase == FlowPhaseEnum.DESIGN.getValue();
        boolean isOrderPhase = phase == FlowPhaseEnum.ORDER.getValue();

        // 3. 权限校验（审核场景跳过）
        if (!skipPermissionCheck) {
            if (!isAdmin && (!isBusinessRole || !isOrderPhase)) {
                if (isDesigner && isDesignPhase) {
                    throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED,
                            "设计师仅可提交修改申请，不能直接修改订单");
                }
                throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_FIELD_NOT_ALLOWED,
                        "当前角色或订单阶段不允许直接修改订单");
            }
        }

        // 4. 按对象 diff（基于修改人角色判断修改范围）
        List<ObjectChange> changes = new ArrayList<>();

        changes.add(diffOrderInfo(order, dto));
        changes.add(diffPatient(order, dto));
        changes.add(diffDoctor(order, dto));
        changes.add(diffHospital(order, dto));
        changes.add(diffDelivery(order, dto));
        if (dto.getItems() != null) {
            changes.add(diffItems(orderId, dto.getItems()));
        }
        if (dto.getImageDataFileIds() != null || dto.getImageReportFileIds() != null) {
            changes.add(diffImages(orderId, dto.getImageDataFileIds(), dto.getImageReportFileIds()));
        }

        // 5. 过滤无变化
        List<ObjectChange> actualChanges = changes.stream()
            .filter(ObjectChange::isHasChange)
            .collect(Collectors.toList());

        if (actualChanges.isEmpty()) {
            log.info("订单无变更: orderId={}", orderId);
            return;
        }

        // 5.5. 二次验证账户级别限制（防止审核期间账户权限变更）
        for (ObjectChange change : actualChanges) {
            if (OrderModifyObjectType.ORDER_INFO.equals(change.getObjectType())) {
                // 验证 orderType 修改权限
                if (!Objects.equals(order.getOrderType(), dto.getOrderType())) {
                    orderDataValidator.validateOrderType(modifierId, dto.getOrderType());
                    log.info("订单类型修改二次验证通过: orderId={}, modifierId={}, {} -> {}",
                            orderId, modifierId, order.getOrderType(), dto.getOrderType());
                }
                // 验证 businessType 修改权限
                if (!Objects.equals(order.getBusinessType(), dto.getBusinessType())) {
                    orderDataValidator.validateBusinessTypeRestrictions(modifierId, dto.getBusinessType(), null);
                    log.info("业务类型修改二次验证通过: orderId={}, modifierId={}, {} -> {}",
                            orderId, modifierId, order.getBusinessType(), dto.getBusinessType());
                }
                break; // ORDER_INFO 只会有一个，找到后退出
            }
        }

        // 6. 应用变更
        for (ObjectChange change : actualChanges) {
            String objectType = change.getObjectType();
            if (OrderModifyObjectType.PATIENT.equals(objectType) || OrderModifyObjectType.DOCTOR.equals(objectType) ||
                OrderModifyObjectType.HOSPITAL.equals(objectType) || OrderModifyObjectType.DELIVERY.equals(objectType) ||
                OrderModifyObjectType.ORDER_INFO.equals(objectType)) {
                applySimpleObjectChange(order, dto, change);
            } else if (OrderModifyObjectType.ITEMS.equals(objectType)) {
                applyItemsChange(orderId, order.getOrderCode(), dto.getItems());
            } else if (OrderModifyObjectType.IMAGES.equals(objectType)) {
                applyImagesChange(orderId, order.getOrderCode(), dto.getImageDataFileIds(), dto.getImageReportFileIds());
            }
        }

        // 7. 递增版本号并更新订单
        order.setVersion(order.getVersion() + 1);
        if (orderMainMapper.updateById(order) <= 0) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "订单主表更新失败");
        }

        // 8. 所有订单数据更新成功后再记录日志（使用传入的修改人信息）
        for (ObjectChange change : actualChanges) {
            OrderModificationLogEntity logEntity = new OrderModificationLogEntity();
            logEntity.setOrderId(orderId);
            logEntity.setOrderCode(order.getOrderCode());
            logEntity.setApplyId(applyId);
            logEntity.setFieldName(change.getObjectType());
            logEntity.setFieldLabel(change.getObjectLabel());
            logEntity.setOldValue(change.getOldValue());
            logEntity.setNewValue(change.getNewValue());
            logEntity.setModifierId(modifierId);
            logEntity.setModifierName(modifierName);
            orderModificationLogMapper.insert(logEntity);
        }

        log.info("订单修改完成: orderId={}, changeCount={}, version={}", orderId, actualChanges.size(), order.getVersion());
    }

    /**
     * 对比订单基本信息（订单类型/业务类型）
     */
    private ObjectChange diffOrderInfo(OrderMainEntity order, OrderModifyFullDTO dto) {
        List<String> oldParts = new ArrayList<>();
        List<String> newParts = new ArrayList<>();

        if (!Objects.equals(order.getOrderType(), dto.getOrderType())) {
            oldParts.add("订单类型=" + getOrderTypeDisplay(order.getOrderType()));
            newParts.add("订单类型=" + getOrderTypeDisplay(dto.getOrderType()));
        }
        if (!Objects.equals(order.getBusinessType(), dto.getBusinessType())) {
            oldParts.add("业务类型=" + getBusinessTypeDisplay(order.getBusinessType()));
            newParts.add("业务类型=" + getBusinessTypeDisplay(dto.getBusinessType()));
        }
        if (!Objects.equals(order.getIsPostal(), dto.getIsPostal())) {
            oldParts.add("邮寄=" + (order.getIsPostal() != null && order.getIsPostal() == 1 ? "是" : "否"));
            newParts.add("邮寄=" + (dto.getIsPostal() != null && dto.getIsPostal() == 1 ? "是" : "否"));
        }
        if (!Objects.equals(order.getEstimatedCost(), dto.getEstimatedCost())) {
            oldParts.add("预估费用=" + (order.getEstimatedCost() != null ? order.getEstimatedCost() : "无"));
            newParts.add("预估费用=" + (dto.getEstimatedCost() != null ? dto.getEstimatedCost() : "无"));
        }
        if (!Objects.equals(order.getDataEvaluationOpinion(), dto.getDataEvaluationOpinion())) {
            oldParts.add("数据评估意见=" + formatFieldValue(order.getDataEvaluationOpinion()));
            newParts.add("数据评估意见=" + formatFieldValue(dto.getDataEvaluationOpinion()));
        }

        if (oldParts.isEmpty()) {
            return ObjectChange.noChange();
        }
        return ObjectChange.of(OrderModifyObjectType.ORDER_INFO, "订单基本信息",
                String.join("，", oldParts), String.join("，", newParts));
    }

    /**
     * 对比患者信息
     */
    private ObjectChange diffPatient(OrderMainEntity order, OrderModifyFullDTO dto) {
        String oldValue = formatPatient(order.getPatientName(), order.getPatientGender(), order.getPatientAge());
        String newValue = formatPatient(dto.getPatientName(), dto.getPatientGender(), dto.getPatientAge());

        if (oldValue.equals(newValue)) {
            return ObjectChange.noChange();
        }

        return ObjectChange.of(OrderModifyObjectType.PATIENT, "患者信息", oldValue, newValue);
    }

    /**
     * 格式化患者信息
     */
    private String formatPatient(String name, String gender, Integer age) {
        String genderText = "无";
        if (StrUtil.isNotBlank(gender)) {
            com.yigongbao.module.system.dict.vo.DictVO dictVO = dictService.getByDictCode(gender);
            genderText = dictVO != null ? dictVO.getDictName() : gender;
        }
        return String.format("%s(%s,%d岁)",
            StrUtil.blankToDefault(name, "无"),
            genderText,
            age == null ? 0 : age);
    }

    private String getOrderTypeDisplay(Integer orderType) {
        if (orderType == null) {
            return "无";
        }
        com.yigongbao.module.system.dict.vo.DictVO dict =
                dictService.getByDictCode(String.valueOf(orderType));
        return dict != null && StrUtil.isNotBlank(dict.getDictName())
                ? dict.getDictName() : String.valueOf(orderType);
    }

    private String getBusinessTypeDisplay(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            return "无";
        }
        com.yigongbao.module.system.dict.vo.DictVO dict = dictService.getByDictCode(businessType);
        return dict != null && StrUtil.isNotBlank(dict.getDictName())
                ? dict.getDictName() : businessType;
    }

    /**
     * 对比医生信息
     */
    private ObjectChange diffDoctor(OrderMainEntity order, OrderModifyFullDTO dto) {
        String oldValue = formatDoctor(order.getDoctorName(), order.getDoctorPhone());
        String newValue = formatDoctor(dto.getDoctorName(), dto.getDoctorPhone());

        if (oldValue.equals(newValue)) {
            return ObjectChange.noChange();
        }

        return ObjectChange.of(OrderModifyObjectType.DOCTOR, "医生信息", oldValue, newValue);
    }

    /**
     * 格式化医生信息
     */
    private String formatDoctor(String name, String phone) {
        return String.format("%s(%s)",
            StrUtil.blankToDefault(name, "无"),
            StrUtil.blankToDefault(phone, "无"));
    }

    /**
     * 对比医院科室
     */
    private ObjectChange diffHospital(OrderMainEntity order, OrderModifyFullDTO dto) {
        // 对比 ID 是否变化
        boolean hospitalChanged = !Objects.equals(order.getHospitalId(), dto.getHospitalId());
        boolean deptChanged = !Objects.equals(order.getHospitalDeptId(), dto.getHospitalDeptId());

        if (!hospitalChanged && !deptChanged) {
            return ObjectChange.noChange();
        }

        // 解析新的医院和科室名称
        String newHospitalName = order.getHospitalName();
        String newDeptName = order.getHospitalDeptName();

        if (hospitalChanged && dto.getHospitalId() != null) {
            OrgEntity hospital = orgService.getById(dto.getHospitalId());
            if (hospital != null) {
                newHospitalName = hospital.getOrgName();
            }
        }

        if (deptChanged && dto.getHospitalDeptId() != null) {
            HospitalDeptVO dept = hospitalDeptService.getById(dto.getHospitalDeptId());
            if (dept != null) {
                newDeptName = dept.getHospitalDeptName();
            }
        }

        String oldValue = formatHospital(order.getHospitalName(), order.getHospitalDeptName());
        String newValue = formatHospital(newHospitalName, newDeptName);

        return ObjectChange.of(OrderModifyObjectType.HOSPITAL, "医院科室", oldValue, newValue);
    }

    /**
     * 格式化医院科室
     */
    private String formatHospital(String hospitalName, String deptName) {
        return String.format("%s-%s",
            StrUtil.blankToDefault(hospitalName, "无"),
            StrUtil.blankToDefault(deptName, "无"));
    }

    /**
     * 对比交付信息
     */
    private ObjectChange diffDelivery(OrderMainEntity order, OrderModifyFullDTO dto) {
        String oldValue = formatDelivery(order.getNeedsPhysicalDelivery(), order.getPostalAddress(), order.getIsUrgent());
        String newValue = formatDelivery(dto.getNeedsPhysicalDelivery(), dto.getPostalAddress(), dto.getIsUrgent());

        if (oldValue.equals(newValue)) {
            return ObjectChange.noChange();
        }

        return ObjectChange.of(OrderModifyObjectType.DELIVERY, "交付信息", oldValue, newValue);
    }

    private void validatePhysicalDeliveryValue(Integer value) {
        if (value != null && !PhysicalDeliveryConstants.isSupported(value)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_INVALID);
        }
    }

    private void validatePhysicalDeliveryChange(OrderMainEntity order, Integer newValue) {
        if (newValue == null || !PhysicalDeliveryConstants.needsProduction(order.getNeedsPhysicalDelivery())
                || !PhysicalDeliveryConstants.isNoPhysicalDelivery(newValue)) {
            return;
        }
        if (!Objects.equals(order.getNeedsPhysicalDelivery(), newValue)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN);
        }
    }

    /**
     * 格式化交付信息
     */
    private String formatDelivery(Integer needsPhysicalDelivery, String address, Integer isUrgent) {
        String deliveryText = PhysicalDeliveryConstants.getDisplayName(needsPhysicalDelivery);
        String addressText = StrUtil.isNotBlank(address) ? "，地址:" + address : "";
        String urgentText = (isUrgent != null && isUrgent == 1) ? "，加急" : "";
        return deliveryText + addressText + urgentText;
    }

    /**
     * 对比重建项目（详细模式）
     */
    private ObjectChange diffItems(Long orderId, List<OrderItemDraftItemDTO> newItems) {
        // 查询当前项目
        List<OrderItemEntity> oldItems = orderItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .eq(OrderItemEntity::getIsDeleted, 0)
        );

        // 构建旧项目映射
        Map<Long, OrderItemEntity> oldItemMap = oldItems.stream()
            .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        // 构建新项目映射
        Map<Long, OrderItemDraftItemDTO> newItemMap = newItems.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(OrderItemDraftItemDTO::getId, item -> item));

        List<String> changes = new ArrayList<>();

        // 检查修改和删除
        for (OrderItemEntity oldItem : oldItems) {
            OrderItemDraftItemDTO newItem = newItemMap.get(oldItem.getId());
            if (newItem == null) {
                // 删除
                changes.add("删除[" + oldItem.getProjectName() + "]");
            } else {
                // 修改
                String changeDesc = diffSingleItem(oldItem, newItem);
                if (StrUtil.isNotBlank(changeDesc)) {
                    changes.add("修改[" + changeDesc + "]");
                }
            }
        }

        // 检查新增
        for (OrderItemDraftItemDTO newItem : newItems) {
            if (newItem.getId() == null) {
                changes.add("新增[" + newItem.getProjectName() + "]");
            }
        }

        if (changes.isEmpty()) {
            return ObjectChange.noChange();
        }

        String oldValue = oldItems.size() + "个项目";
        String newValue = String.join("；", changes);
        return ObjectChange.of(OrderModifyObjectType.ITEMS, "重建项目", oldValue, newValue);
    }

    /**
     * 对比单个重建项目
     */
    private String diffSingleItem(OrderItemEntity oldItem, OrderItemDraftItemDTO newItem) {
        List<String> changes = new ArrayList<>();

        // 对比核心字段（项目名称）
        if (!Objects.equals(oldItem.getProjectName(), newItem.getProjectName())) {
            changes.add(oldItem.getProjectName() + "→" + newItem.getProjectName());
        }

        // 对比描述字段
        List<String> descChanges = new ArrayList<>();

        if (!Objects.equals(oldItem.getProjectDesc(), newItem.getProjectDesc())) {
            descChanges.add("项目说明：" + formatFieldValue(oldItem.getProjectDesc()) +
                          "→" + formatFieldValue(newItem.getProjectDesc()));
        }

        if (!Objects.equals(oldItem.getFormingRequirement(), newItem.getFormingRequirement())) {
            descChanges.add("成形需求：" + formatFieldValue(oldItem.getFormingRequirement()) +
                          "→" + formatFieldValue(newItem.getFormingRequirement()));
        }

        if (!Objects.equals(oldItem.getOtherRequirement(), newItem.getOtherRequirement())) {
            descChanges.add("其他要求：" + formatFieldValue(oldItem.getOtherRequirement()) +
                          "→" + formatFieldValue(newItem.getOtherRequirement()));
        }

        if (!descChanges.isEmpty()) {
            changes.addAll(descChanges);
        }

        if (changes.isEmpty()) {
            return "";
        }

        // 如果只有核心字段变化，直接返回
        if (changes.size() == 1 && !changes.get(0).contains("：")) {
            return changes.get(0);
        }

        // 如果有描述字段变化，格式化输出
        String projectName = newItem.getProjectName();
        if (!Objects.equals(oldItem.getProjectName(), newItem.getProjectName())) {
            projectName = oldItem.getProjectName() + "→" + newItem.getProjectName();
        }

        return projectName + "：" + String.join("，", changes.stream()
            .filter(c -> c.contains("："))
            .collect(Collectors.toList()));
    }

    /**
     * 格式化字段值
     */
    private String formatFieldValue(String value) {
        if (StrUtil.isBlank(value)) {
            return "无";
        }
        if (value.length() > MAX_FIELD_VALUE_LENGTH) {
            return value.substring(0, MAX_FIELD_VALUE_LENGTH) + "...";
        }
        return value;
    }

    /**
     * 对比影像文件
     */
    private ObjectChange diffImages(Long orderId, List<String> newDataFileIds, List<String> newReportFileIds) {
        // 查询当前影像文件
        List<OrderFileEntity> oldFiles = orderFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderFileEntity>()
                .eq(OrderFileEntity::getOrderId, orderId)
                .eq(OrderFileEntity::getIsDeleted, 0)
        );

        // 分离数据文件和报告文件
        long oldDataCount = oldFiles.stream()
            .filter(f -> DictCodeConstants.ORDER_FILE_CATEGORY_DCM.equals(f.getFileCategory()))
            .count();
        long oldReportCount = oldFiles.stream()
            .filter(f -> DictCodeConstants.ORDER_FILE_CATEGORY_REPORT.equals(f.getFileCategory()))
            .count();

        int newDataCount = newDataFileIds == null ? 0 : newDataFileIds.size();
        int newReportCount = newReportFileIds == null ? 0 : newReportFileIds.size();

        if (oldDataCount == newDataCount && oldReportCount == newReportCount) {
            return ObjectChange.noChange();
        }

        String oldValue = String.format("影像数据%d个，影像报告%d个", oldDataCount, oldReportCount);
        String newValue = String.format("影像数据%d个，影像报告%d个", newDataCount, newReportCount);
        return ObjectChange.of(OrderModifyObjectType.IMAGES, "影像文件", oldValue, newValue);
    }

    /**
     * 应用简单对象变更
     */
    private void applySimpleObjectChange(OrderMainEntity order, OrderModifyFullDTO dto, ObjectChange change) {
        if (!change.isHasChange()) {
            return;
        }

        String objectType = change.getObjectType();
        switch (objectType) {
            case OrderModifyObjectType.PATIENT:
                log.info("修改患者信息: orderId={}, {} -> {}", order.getId(), change.getOldValue(), change.getNewValue());
                order.setPatientName(dto.getPatientName());
                order.setPatientGender(dto.getPatientGender());
                order.setPatientAge(dto.getPatientAge());
                break;
            case OrderModifyObjectType.DOCTOR:
                log.info("修改医生信息: orderId={}, {} -> {}", order.getId(), change.getOldValue(), change.getNewValue());
                // 使用 OrderDataValidator 处理医生信息（支持 quickAdd 和历史记录更新）
                orderDataValidator.validateAndFillForModify(order, null,
                    dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone());
                break;
            case OrderModifyObjectType.HOSPITAL:
                log.info("修改医院科室: orderId={}, {} -> {}", order.getId(), change.getOldValue(), change.getNewValue());
                order.setHospitalId(dto.getHospitalId());
                order.setHospitalDeptId(dto.getHospitalDeptId());
                // 从数据库解析医院和科室名称，不信任前端
                if (dto.getHospitalId() != null) {
                    OrgEntity hospital = orgService.getById(dto.getHospitalId());
                    if (hospital != null) {
                        order.setHospitalName(hospital.getOrgName());
                    }
                }
                if (dto.getHospitalDeptId() != null) {
                    HospitalDeptVO dept = hospitalDeptService.getById(dto.getHospitalDeptId());
                    if (dept != null) {
                        order.setHospitalDeptName(dept.getHospitalDeptName());
                    }
                }
                break;
            case OrderModifyObjectType.DELIVERY:
                log.info("修改交付信息: orderId={}, {} -> {}", order.getId(), change.getOldValue(), change.getNewValue());
                order.setNeedsPhysicalDelivery(dto.getNeedsPhysicalDelivery());
                order.setPostalAddress(dto.getPostalAddress());
                order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
                order.setIsUrgent(dto.getIsUrgent());
                break;
            case OrderModifyObjectType.ORDER_INFO:
                log.info("修改订单基本信息: orderId={}, {} -> {}", order.getId(), change.getOldValue(), change.getNewValue());
                order.setOrderType(dto.getOrderType());
                order.setBusinessType(dto.getBusinessType());
                order.setIsPostal(dto.getIsPostal());
                order.setEstimatedCost(dto.getEstimatedCost());
                order.setDataEvaluationOpinion(dto.getDataEvaluationOpinion());
                break;
        }
    }

    /**
     * 应用重建项目变更
     */
    private void applyItemsChange(Long orderId, String orderCode, List<OrderItemDraftItemDTO> newItems) {
        // 查询当前项目
        List<OrderItemEntity> oldItems = orderItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .eq(OrderItemEntity::getIsDeleted, 0)
        );

        Map<Long, OrderItemEntity> oldItemMap = oldItems.stream()
            .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        Map<Long, OrderItemDraftItemDTO> newItemMap = newItems.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(OrderItemDraftItemDTO::getId, item -> item));

        // 删除不在新列表中的项目
        for (OrderItemEntity oldItem : oldItems) {
            if (!newItemMap.containsKey(oldItem.getId())) {
                if (orderItemMapper.deleteById(oldItem.getId()) <= 0) {
                    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "订单项目删除失败");
                }
            }
        }

        // 更新或新增项目
        for (OrderItemDraftItemDTO newItem : newItems) {
            if (newItem.getId() != null) {
                // 更新
                OrderItemEntity entity = oldItemMap.get(newItem.getId());
                if (entity != null) {
                    entity.setBodyPartId(newItem.getBodyPartId());
                    entity.setBodyPartName(newItem.getBodyPartName());
                    entity.setProjectId(newItem.getProjectId());
                    entity.setProjectName(newItem.getProjectName());
                    entity.setProjectDesc(newItem.getProjectDesc());
                    entity.setFormingRequirement(newItem.getFormingRequirement());
                    entity.setOtherRequirement(newItem.getOtherRequirement());
                    if (orderItemMapper.updateById(entity) <= 0) {
                        throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "订单项目更新失败");
                    }
                }
            } else {
                // 新增
                OrderItemEntity entity = new OrderItemEntity();
                entity.setOrderId(orderId);
                entity.setOrderCode(orderCode);
                entity.setBodyPartId(newItem.getBodyPartId());
                entity.setBodyPartName(newItem.getBodyPartName());
                entity.setProjectId(newItem.getProjectId());
                entity.setProjectName(newItem.getProjectName());
                entity.setProjectDesc(newItem.getProjectDesc());
                entity.setFormingRequirement(newItem.getFormingRequirement());
                entity.setOtherRequirement(newItem.getOtherRequirement());
                if (orderItemMapper.insert(entity) <= 0) {
                    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "订单项目新增失败");
                }
            }
        }
    }

    /**
     * 应用影像文件变更
     */
    private void applyImagesChange(Long orderId, String orderCode, List<String> newDataFileIds, List<String> newReportFileIds) {
        // 查询当前影像文件
        List<OrderFileEntity> oldFiles = orderFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderFileEntity>()
                .eq(OrderFileEntity::getOrderId, orderId)
                .eq(OrderFileEntity::getIsDeleted, 0)
        );

        // 收集旧文件ID
        List<String> oldDataFileIds = oldFiles.stream()
            .filter(f -> DictCodeConstants.ORDER_FILE_CATEGORY_DCM.equals(f.getFileCategory()))
            .map(OrderFileEntity::getFileId)
            .collect(Collectors.toList());

        List<String> oldReportFileIds = oldFiles.stream()
            .filter(f -> DictCodeConstants.ORDER_FILE_CATEGORY_REPORT.equals(f.getFileCategory()))
            .map(OrderFileEntity::getFileId)
            .collect(Collectors.toList());

        // 删除不在新列表中的文件
        for (OrderFileEntity oldFile : oldFiles) {
            boolean shouldDelete = false;
            if (DictCodeConstants.ORDER_FILE_CATEGORY_DCM.equals(oldFile.getFileCategory())) {
                shouldDelete = newDataFileIds == null || !newDataFileIds.contains(oldFile.getFileId());
            } else if (DictCodeConstants.ORDER_FILE_CATEGORY_REPORT.equals(oldFile.getFileCategory())) {
                shouldDelete = newReportFileIds == null || !newReportFileIds.contains(oldFile.getFileId());
            }
            if (shouldDelete) {
                if (orderFileMapper.deleteById(oldFile.getId()) <= 0) {
                    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "订单文件删除失败");
                }
            }
        }

        // 新增数据文件
        if (newDataFileIds != null) {
            for (String fileId : newDataFileIds) {
                if (!oldDataFileIds.contains(fileId)) {
                    OrderFileEntity entity = new OrderFileEntity();
                    entity.setOrderId(orderId);
                    entity.setOrderCode(orderCode);
                    entity.setFileId(fileId);
                    entity.setFileCategory(DictCodeConstants.ORDER_FILE_CATEGORY_DCM);
                    if (orderFileMapper.insert(entity) <= 0) {
                        throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "影像数据文件新增失败");
                    }
                }
            }
        }

        // 新增报告文件
        if (newReportFileIds != null) {
            for (String fileId : newReportFileIds) {
                if (!oldReportFileIds.contains(fileId)) {
                    OrderFileEntity entity = new OrderFileEntity();
                    entity.setOrderId(orderId);
                    entity.setOrderCode(orderCode);
                    entity.setFileId(fileId);
                    entity.setFileCategory(DictCodeConstants.ORDER_FILE_CATEGORY_REPORT);
                    if (orderFileMapper.insert(entity) <= 0) {
                        throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "影像报告文件新增失败");
                    }
                }
            }
        }
    }
}
