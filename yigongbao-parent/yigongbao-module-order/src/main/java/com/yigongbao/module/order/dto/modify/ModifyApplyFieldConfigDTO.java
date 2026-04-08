package com.yigongbao.module.order.dto.modify;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 订单修改申请字段配置 DTO
 * 对应 sys_config 中 order.modify.field.config 的 JSON 结构
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ModifyApplyFieldConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 各申请类型的字段配置，key 为字典编码（如 "14.1"）
     */
    private Map<String, TypeConfig> typeConfigs;

    /**
     * 获取指定申请类型的字段配置
     *
     * @param typeCode 申请类型字典编码
     * @return 类型配置，未找到返回 null
     */
    public TypeConfig getTypeConfig(String typeCode) {
        if (typeConfigs == null) return null;
        return typeConfigs.get(typeCode);
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
    }

    /**
     * 单个字段的配置
     */
    @Data
    public static class FieldConfig {

        /**
         * 字段名（与 ExecuteModificationDTO 属性名对应）
         */
        private String field;

        /**
         * 字段中文名（前端展示用）
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
    }
}
