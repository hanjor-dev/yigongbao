package com.yigongbao.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnConfigMergeUtilTest {

    @Test
    void mergeMissingColumns_appendsNewDefaultsAndPreservesUserSettings() {
        Column user = new Column("recordNo", false, 1, 220);
        Column newDefault = new Column("designerRemark", true, 2, 180);

        List<Column> result = ColumnConfigMergeUtil.mergeMissingColumns(
                List.of(user), List.of(user, newDefault),
                Column::field, Column::copy, Column::sort, Column::withSort);

        assertEquals(2, result.size());
        assertEquals("recordNo", result.get(0).field());
        assertEquals(false, result.get(0).visible());
        assertEquals(220, result.get(0).width());
        assertEquals("designerRemark", result.get(1).field());
        assertEquals(2, result.get(1).sort());
    }

    @Test
    void mergeMissingColumns_avoidsDuplicateFieldsAndSortConflicts() {
        Column user = new Column("recordNo", true, 5, 160);
        Column newDefault = new Column("newField", true, 1, 120);

        List<Column> result = ColumnConfigMergeUtil.mergeMissingColumns(
                List.of(user), List.of(user, newDefault),
                Column::field, Column::copy, Column::sort, Column::withSort);

        assertEquals(2, result.size());
        assertEquals(6, result.get(1).sort());
    }

    private record Column(String field, Boolean visible, Integer sort, Integer width) {
        private Column copy() {
            return new Column(field, visible, sort, width);
        }

        private Column withSort(Integer newSort) {
            return new Column(field, visible, newSort, width);
        }
    }
}
