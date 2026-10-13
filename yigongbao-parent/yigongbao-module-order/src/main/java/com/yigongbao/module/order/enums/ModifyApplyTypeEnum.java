package com.yigongbao.module.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单修改类型枚举
 * 字典编码对应 sys_dict group=14：14.1（全量修改）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Getter
@AllArgsConstructor
public enum ModifyApplyTypeEnum {

    FULL("14.1", "全量修改");

    /**
     * 字典编码（存储到数据库，对应 sys_dict.dict_code）
     */
    private final String dictCode;

    /**
     * 类型名称（中文描述）
     */
    private final String name;

    /**
     * 获取整数编码（用于数据库存储）
     * 将字典编码转换为整数，如 "14.1" -> 141
     */
    public Integer getCode() {
        return Integer.parseInt(dictCode.replace(".", ""));
    }

    /**
     * 根据字典编码获取枚举
     *
     * @param dictCode 字典编码
     * @return 枚举实例，未找到返回 null
     */
    public static ModifyApplyTypeEnum getByDictCode(String dictCode) {
        return FULL.getDictCode().equals(dictCode) ? FULL : null;
    }
}
