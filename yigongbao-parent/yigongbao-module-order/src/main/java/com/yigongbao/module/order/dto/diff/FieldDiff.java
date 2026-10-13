package com.yigongbao.module.order.dto.diff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 单字段差异
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 字段标签（中文）
     */
    private String fieldLabel;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String newValue;

    /**
     * 旧值显示名称（可选，用于外键）
     */
    private String oldDisplay;

    /**
     * 新值显示名称（可选，用于外键）
     */
    private String newDisplay;

    /**
     * 4参数构造器(不包含 oldDisplay/newDisplay)
     *
     * @param fieldName 字段名
     * @param fieldLabel 字段标签(中文)
     * @param oldValue 旧值
     * @param newValue 新值
     */
    public FieldDiff(String fieldName, String fieldLabel, String oldValue, String newValue) {
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
