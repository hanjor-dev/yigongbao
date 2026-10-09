package com.yigongbao.common.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 列配置兼容合并工具。
 */
public final class ColumnConfigMergeUtil {

    private ColumnConfigMergeUtil() {
    }

    /**
     * 保留用户已有列配置，只追加默认配置中尚不存在的列。
     * 新增列的排序号从用户配置的最大排序号之后开始，避免排序冲突。
     */
    public static <T> List<T> mergeMissingColumns(
            List<T> userColumns,
            List<T> defaultColumns,
            Function<T, String> fieldGetter,
            Function<T, T> copier,
            Function<T, Integer> sortGetter,
            BiFunction<T, Integer, T> sortSetter) {
        List<T> result = new ArrayList<>();
        if (userColumns != null) {
            result.addAll(userColumns);
        }

        Set<String> fields = new HashSet<>();
        int maxSort = 0;
        for (T column : result) {
            if (column == null) {
                continue;
            }
            String field = fieldGetter.apply(column);
            if (field != null) {
                fields.add(field);
            }
            Integer sort = sortGetter.apply(column);
            if (sort != null) {
                maxSort = Math.max(maxSort, sort);
            }
        }

        if (defaultColumns == null) {
            return result;
        }
        for (T defaultColumn : defaultColumns) {
            if (defaultColumn == null || fields.contains(fieldGetter.apply(defaultColumn))) {
                continue;
            }
            fields.add(fieldGetter.apply(defaultColumn));
            result.add(sortSetter.apply(copier.apply(defaultColumn), ++maxSort));
        }
        return result;
    }
}
