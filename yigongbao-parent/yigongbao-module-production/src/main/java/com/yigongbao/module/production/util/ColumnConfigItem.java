package com.yigongbao.module.production.util;

/**
 * 可保存列表列配置项的统一访问接口。
 */
public interface ColumnConfigItem {

    String getField();

    String getLabel();

    Boolean getVisible();

    Integer getSort();

    Integer getWidth();

    String getFixed();
}
