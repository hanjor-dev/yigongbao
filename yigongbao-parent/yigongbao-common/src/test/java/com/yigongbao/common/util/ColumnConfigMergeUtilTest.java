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

    @Test
    void normalizeOrderCodeColumns_replacesOldColumnAndRemovesDuplicate() {
        MutableColumn oldColumn = new MutableColumn("orderCode", "订单编号");
        MutableColumn publicColumn = new MutableColumn("publicOrderCode", "虚拟单号");
        MutableColumn other = new MutableColumn("recordNo", "记录号");

        List<MutableColumn> result = ColumnConfigMergeUtil.normalizeOrderCodeColumns(
                List.of(other, oldColumn, publicColumn),
                MutableColumn::field,
                MutableColumn::setField,
                MutableColumn::setLabel);

        assertEquals(2, result.size());
        assertEquals("publicOrderCode", result.get(1).field());
        assertEquals("订单号", result.get(1).label());
    }

    private record Column(String field, Boolean visible, Integer sort, Integer width) {
        private Column copy() {
            return new Column(field, visible, sort, width);
        }

        private Column withSort(Integer newSort) {
            return new Column(field, visible, newSort, width);
        }
    }

    private static final class MutableColumn {
        private String field;
        private String label;

        private MutableColumn(String field, String label) {
            this.field = field;
            this.label = label;
        }

        private String field() {
            return field;
        }

        private String label() {
            return label;
        }

        private void setField(String field) {
            this.field = field;
        }

        private void setLabel(String label) {
            this.label = label;
        }
    }
}
