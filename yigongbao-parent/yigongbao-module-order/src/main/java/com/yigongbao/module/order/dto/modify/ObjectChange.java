package com.yigongbao.module.order.dto.modify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 对象变更记录
 * 用于记录业务对象的变更信息
 *
 * @author hanjor
 * @date 2026-05-22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectChange implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对象类型（patient/doctor/hospital/delivery/items/images）
     */
    private String objectType;

    /**
     * 对象名称（患者信息/医生信息等）
     */
    private String objectLabel;

    /**
     * 旧值描述
     */
    private String oldValue;

    /**
     * 新值描述
     */
    private String newValue;

    /**
     * 是否有变化
     */
    private boolean hasChange;

    /**
     * 创建无变化的记录
     */
    public static ObjectChange noChange() {
        return new ObjectChange(null, null, null, null, false);
    }

    /**
     * 创建有变化的记录
     */
    public static ObjectChange of(String objectType, String objectLabel, String oldValue, String newValue) {
        return new ObjectChange(objectType, objectLabel, oldValue, newValue, true);
    }
}
