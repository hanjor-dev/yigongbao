package com.yigongbao.module.design.enums;

import lombok.Getter;

/** 追加设计批次状态。 */
@Getter
public enum DesignPackageBatchStatus {
    UPLOADING,
    PRINT_INFO_EDITING,
    COMPLETED,
    CANCELLED
}
