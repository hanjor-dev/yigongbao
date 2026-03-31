package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 患者性别枚举
 * dict_code 对应 sys_dict.dict_code
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderPatientGenderEnum {

    /**
     * 男
     * dict_code = "12.1"
     */
    MALE("12.1", "男"),

    /**
     * 女
     * dict_code = "12.2"
     */
    FEMALE("12.2", "女");

    /**
     * 字典编码（dict_code）
     */
    private final String dictCode;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据 dictCode 获取枚举
     *
     * @param dictCode 字典编码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static OrderPatientGenderEnum getByDictCode(String dictCode) {
        if (dictCode == null) {
            return null;
        }
        for (OrderPatientGenderEnum enumItem : OrderPatientGenderEnum.values()) {
            if (enumItem.getDictCode().equals(dictCode)) {
                return enumItem;
            }
        }
        return null;
    }
}
