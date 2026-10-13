package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 * 定义系统中所有的操作类型，用于操作日志记录
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Getter
@AllArgsConstructor
public enum OperationTypeEnum {

    CREATE(1, "新增"),
    UPDATE(2, "更新"),
    DELETE(3, "删除"),
    AUDIT(4, "审核"),
    SUBMIT(5, "提交"),
    CANCEL(6, "取消"),
    EXPORT(7, "导出"),
    IMPORT(8, "导入"),
    ASSIGN(9, "分配"),
    TRANSFER(10, "转移"),
    ENABLE(11, "启用"),
    DISABLE(12, "禁用"),
    UPLOAD(13, "上传"),
    DOWNLOAD(14, "下载"),
    LOGIN(15, "登录"),
    LOGOUT(16, "登出");

    /**
     * 操作类型编码
     */
    private final Integer code;

    /**
     * 操作类型描述
     */
    private final String description;
}
