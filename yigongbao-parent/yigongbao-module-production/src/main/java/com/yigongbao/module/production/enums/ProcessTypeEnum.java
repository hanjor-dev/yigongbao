package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工序类型枚举
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
public enum ProcessTypeEnum {
    PRINT("print", "3D打印成型", 1),
    WASH("wash", "酒精初洗（含打磨）", 2),
    CURE("cure", "UV固化", 3),
    CLEAN_DRY("clean_dry", "超声清洗+干燥", 4),
    PACK("pack", "包装", 5);

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
    private final Integer order;

    /**
     * 根据code获取枚举
     */
    public static ProcessTypeEnum getByCode(String code) {
        if (code == null) return null;
        for (ProcessTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
