package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单业务类型枚举
 * dict_code 对应 sys_dict.dict_code
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderBusinessTypeEnum {

    /**
     * 业务
     * dict_code = "11.1"
     */
    BUSINESS("11.1", "业务"),

    /**
     * 测试
     * dict_code = "11.2"
     */
    TEST("11.2", "测试"),

    /**
     * 试用
     * dict_code = "11.3"
     */
    TRIAL("11.3", "试用"),

    /**
     * 代理
     * dict_code = "11.4"
     */
    AGENT("11.4", "代理");

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
    public static OrderBusinessTypeEnum getByDictCode(String dictCode) {
        if (dictCode == null) {
            return null;
        }
        for (OrderBusinessTypeEnum enumItem : OrderBusinessTypeEnum.values()) {
            if (enumItem.getDictCode().equals(dictCode)) {
                return enumItem;
            }
        }
        return null;
    }
}
