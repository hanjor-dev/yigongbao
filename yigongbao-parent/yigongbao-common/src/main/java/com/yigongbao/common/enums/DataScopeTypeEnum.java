package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围类型枚举
 * 用于定义角色的数据权限范围，控制用户可以查看和操作哪些数据
 *
 * @author hanjor
 * @date 2026-04-03
 */
@Getter
@AllArgsConstructor
public enum DataScopeTypeEnum {

    /**
     * 仅自己创建的数据
     * 用户只能查看和操作自己创建的数据（如自己提交的订单）
     */
    SELF("self", "仅自己"),

    /**
     * 医院范围数据
     * 用户只能查看和操作其关联医院范围内的数据
     * 医院范围通过 sys_user_hospital 表关联配置
     */
    HOSPITALS("hospitals", "医院范围"),

    /**
     * 本机构数据
     * 用户可以查看和操作本机构内的所有数据
     */
    ORG("org", "本机构"),

    /**
     * 全部数据
     * 用户可以查看和操作全部数据，不受任何范围限制
     */
    ALL("all", "全部");

    /**
     * 枚举编码，对应数据库 data_scope_type 字段值
     */
    private final String code;

    /**
     * 枚举描述，用于前端显示
     */
    private final String desc;

    /**
     * 根据 code 获取枚举
     *
     * @param code 枚举编码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static DataScopeTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataScopeTypeEnum enumItem : DataScopeTypeEnum.values()) {
            if (enumItem.getCode().equals(code)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 根据 code 获取枚举，未找到时返回默认值 ORG
     *
     * @param code 枚举编码
     * @return 对应的枚举实例，未找到则返回 ORG
     */
    public static DataScopeTypeEnum getByCodeOrDefault(String code) {
        DataScopeTypeEnum result = getByCode(code);
        return result != null ? result : ORG;
    }

    /**
     * 根据 code 获取描述文字，未找到时返回 code 本身
     *
     * @param code 枚举编码
     * @return 对应的描述文字
     */
    public static String getDescByCode(String code) {
        if (code == null) {
            return "";
        }
        DataScopeTypeEnum result = getByCode(code);
        return result != null ? result.getDesc() : code;
    }
}
