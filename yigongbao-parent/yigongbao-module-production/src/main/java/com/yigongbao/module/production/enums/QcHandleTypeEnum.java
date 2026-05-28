package com.yigongbao.module.production.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QcHandleTypeEnum {
    REWORK_TO_PRINT("REWORK_TO_PRINT", "回退到打印"),
    ASSIGN_PROCESS("ASSIGN_PROCESS", "指定工序重做");

    private final String code;
    private final String desc;
}
