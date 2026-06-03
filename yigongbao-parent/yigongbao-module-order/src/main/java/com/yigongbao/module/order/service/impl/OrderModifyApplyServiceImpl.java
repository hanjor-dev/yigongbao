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
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.ModifyApplyFieldConfigDTO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
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
    private final com.yigongbao.flow.service.FlowOrderService flowOrderService;

    // ==================== 阶段判断 ====================

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



    // ==================== 执行修改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void directModify(Long orderId, ExecuteModifyDTO dto) {
        // 1. 查询订单
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 2. 根据阶段判断允许的修改类型
        Set<String> allowedTypes = determineAllowedTypesByPhase(order.getPhase());

        // 3. 加载字段配置
        ModifyApplyFieldConfigDTO fieldConfig = loadFieldConfig();

        // 4. 校验字段白名单
        if (dto != null && dto.getInfoFields() != null && !dto.getInfoFields().isEmpty()) {
            ModifyApplyFieldConfigDTO.TypeConfig infoTypeConfig =
                    fieldConfig.getTypeConfig(ModifyApplyTypeEnum.INFO.getDictCode());
            validateInfoFieldsInWhitelist(dto.getInfoFields(), infoTypeConfig);
        }

        // 5. 构建修改内容 Map
        Map<String, Object> modifications = buildModificationsMap(dto);

        // 6. 获取当前操作人
        Long modifierId = StpUtil.getLoginIdAsLong();
        String modifierName = getCurrentUserName();

        // 7. 处理基础信息修改（14.1）
        boolean infoModified = false;
        if (allowedTypes.contains(ModifyApplyTypeEnum.INFO.getDictCode())
                && !modifications.isEmpty()) {
            infoModified = processInfoModification(order, modifications, null,
                    modifierId, modifierName, fieldConfig);
        }

        // 8. 处理重建项目修改（14.3）
        if (allowedTypes.contains(ModifyApplyTypeEnum.ITEM.getDictCode())
                && modifications.containsKey("items")) {
            processItemModification(order, modifications, null,
                    modifierId, modifierName, fieldConfig);
        }

        // 9. 处理影像文件修改（14.2）
        if (allowedTypes.contains(ModifyApplyTypeEnum.IMAGE.getDictCode())) {
            processImageModification(order, modifications, null,
                    modifierId, modifierName);
        }

        // 10. 仅 INFO 类型修改了 order 实体字段时才回写 DB
        if (infoModified) {
            orderMainMapper.updateById(order);
        }

        // 递增版本号，使持有旧版本的审核操作失效
        flowOrderService.incrementVersion(orderId);

        log.info("直接修改订单: orderId={}", orderId);
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
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto) {
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

    private ModificationLogVO toLogVO(OrderModificationLogEntity entity) {
        ModificationLogVO vo = new ModificationLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
