package com.yigongbao.module.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单修改申请类型枚举
 * 对应 order_modify_apply.apply_type_codes 字段，存储字典编码
 * 字典编码对应 sys_dict group=14：14.1（基础信息）/ 14.2（影像文件）/ 14.3（重建项目）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Getter
@AllArgsConstructor
public enum ModifyApplyTypeEnum {

    INFO("14.1", "基础信息"),
    IMAGE("14.2", "影像文件"),
    ITEM("14.3", "重建项目");

    /**
     * 字典编码（存储到数据库，对应 sys_dict.dict_code）
     */
    private final String dictCode;

    /**
     * 类型名称（中文描述）
     */
    private final String name;

    /**
     * 根据字典编码获取枚举
     *
     * @param dictCode 字典编码
     * @return 枚举实例，未找到返回 null
     */
    public static ModifyApplyTypeEnum getByDictCode(String dictCode) {
        if (dictCode == null) {
            return null;
        }
        for (ModifyApplyTypeEnum type : values()) {
            if (type.getDictCode().equals(dictCode)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 将逗号分隔的字典编码转换为中文描述（顿号连接）
     * 例如："14.1,14.3" → "基础信息、重建项目"
     *
     * @param dictCodes 逗号分隔的字典编码
     * @return 中文名称拼接
     */
    public static String toNamesText(String dictCodes) {
        if (dictCodes == null || dictCodes.isBlank()) {
            return "";
        }
        return Arrays.stream(dictCodes.split(","))
                .map(String::trim)
                .map(ModifyApplyTypeEnum::getByDictCode)
                .filter(Objects::nonNull)
                .map(ModifyApplyTypeEnum::getName)
                .collect(Collectors.joining("、"));
    }
}
