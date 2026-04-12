package com.yigongbao.module.order.dto.modify;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单修改申请字段配置 DTO
 * <p>
 * 对应 sys_config 中 order.modify.field.config 的 JSON 结构。
 * 配置 JSON 的顶层 key 即申请类型字典编码（如 "14.1"），直接映射为 Map 条目。
 * <p>
 * JSON 格式示例：
 * <pre>{@code
 * {
 *   "14.1": { "name": "基础信息", "fields": [...] },
 *   "14.2": { "name": "影像文件", "fields": [...] },
 *   "14.3": { "name": "重建项目", "fields": [...] }
 * }
 * }</pre>
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ModifyApplyFieldConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 各申请类型的字段配置，key 为字典编码（如 "14.1"）
     * 使用 @JsonAnySetter 将 JSON 顶层未知 key 动态注入此 Map
     */
    private Map<String, TypeConfig> typeConfigs = new HashMap<>();

    /**
     * Jackson 动态 key 反序列化：JSON 顶层每个 key-value 对都会调用此方法
     */
    @JsonAnySetter
    public void addTypeConfig(String typeCode, TypeConfig config) {
        this.typeConfigs.put(typeCode, config);
    }

    /**
     * 获取指定申请类型的字段配置
     *
     * @param typeCode 申请类型字典编码（如 "14.1"）
     * @return 类型配置，未找到返回 null
     */
    public TypeConfig getTypeConfig(String typeCode) {
        return typeConfigs.get(typeCode);
    }

    /**
     * 获取指定申请类型下某字段的 label（中文名）
     * 找不到时返回 fieldName 本身作为兜底
     *
     * @param typeCode  申请类型字典编码
     * @param fieldName 字段名
     * @return 字段 label
     */
    public String getFieldLabel(String typeCode, String fieldName) {
        TypeConfig typeConfig = getTypeConfig(typeCode);
        if (typeConfig == null || typeConfig.getFields() == null) {
            return fieldName;
        }
        return typeConfig.getFields().stream()
                .filter(f -> fieldName.equals(f.getField()))
                .map(FieldConfig::getLabel)
                .findFirst()
                .orElse(fieldName);
    }

    /**
     * 获取指定申请类型下所有允许的字段名列表
     *
     * @param typeCode 申请类型字典编码
     * @return 允许字段名列表，未配置时返回空列表
     */
    public List<String> getAllowedFields(String typeCode) {
        TypeConfig typeConfig = getTypeConfig(typeCode);
        if (typeConfig == null || typeConfig.getFields() == null) {
            return List.of();
        }
        return typeConfig.getFields().stream()
                .map(FieldConfig::getField)
                .toList();
    }

    /**
     * 单种申请类型的字段配置
     */
    @Data
    public static class TypeConfig {

        /**
         * 类型名称（如 "基础信息"）
         */
        private String name;

        /**
         * 允许修改的字段列表
         */
        private List<FieldConfig> fields;

        /**
         * 判断字段名是否在允许修改的字段列表中
         *
         * @param fieldName 字段名
         * @return 是否在范围内
         */
        public boolean containsField(String fieldName) {
            if (fields == null) return false;
            return fields.stream().anyMatch(f -> fieldName.equals(f.getField()));
        }

        /**
         * 按 group 名称筛选字段列表
         * 用于区分需要特殊处理的字段组（如 "hospital_doctor" 需要调用 validateAndFillForModify）
         *
         * @param group 分组名称，null 表示返回所有无分组字段（group 为 null 的字段）
         * @return 该分组下的字段列表
         */
        public List<FieldConfig> getFieldsByGroup(String group) {
            if (fields == null) return List.of();
            return fields.stream()
                    .filter(f -> group == null ? f.getGroup() == null : group.equals(f.getGroup()))
                    .toList();
        }

        /**
         * 获取重建项目（14.3）配置中的子字段列表
         * <p>
         * 14.3 的 fields 中有一个 field="items" 的条目，其 subFields 即项目内可修改的字段列表。
         *
         * @return 项目子字段列表，未配置时返回空列表
         */
        public List<FieldConfig> getItemSubFields() {
            if (fields == null) return List.of();
            return fields.stream()
                    .filter(f -> "items".equals(f.getField()))
                    .findFirst()
                    .map(f -> f.getSubFields() != null ? f.getSubFields() : List.<FieldConfig>of())
                    .orElse(List.of());
        }
    }

    /**
     * 单个字段的配置
     */
    @Data
    public static class FieldConfig {

        /**
         * 字段名（与 OrderMainEntity / OrderItemEntity 属性名对应）
         */
        private String field;

        /**
         * 字段中文名（前端展示用，也用于留痕记录的 fieldLabel）
         */
        private String label;

        /**
         * 前端组件类型：text / number / select / switch / textarea / datetime / file / array / autocomplete
         */
        private String type;

        /**
         * 是否必填（预留，暂不使用）
         */
        private Boolean required;

        /**
         * 字段分组标识，用于标记需要特殊处理逻辑的字段
         * 如 "hospital_doctor" 表示该字段修改后需调用 validateAndFillForModify
         */
        private String group;

        /**
         * 子字段列表，仅 field="items" 的配置条目使用
         * 存放重建项目（14.3）内每个 item 可修改的字段
         */
        private List<FieldConfig> subFields;
    }
}
