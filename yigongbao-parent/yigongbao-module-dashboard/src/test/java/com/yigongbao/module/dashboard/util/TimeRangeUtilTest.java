package com.yigongbao.module.dashboard.util;

import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeRangeUtil 单元测试
 */
class TimeRangeUtilTest {

    @Test
    void testGetStartAndEndTime_Today() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(TimeRangeEnum.TODAY);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertEquals(0, range[0].getHour());
        assertEquals(0, range[0].getMinute());
        assertEquals(23, range[1].getHour());
        assertEquals(59, range[1].getMinute());
    }

    @Test
    void testGetStartAndEndTime_Week() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(TimeRangeEnum.WEEK);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertTrue(range[0].isBefore(range[1]));
    }

    @Test
    void testGetStartAndEndTime_Month() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(TimeRangeEnum.MONTH);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertEquals(1, range[0].getDayOfMonth());
    }

    @Test
    void testGetStartAndEndTime_Quarter() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(TimeRangeEnum.QUARTER);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertTrue(range[0].isBefore(range[1]));
    }

    @Test
    void testGetStartAndEndTime_Year() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(TimeRangeEnum.YEAR);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertEquals(1, range[0].getMonthValue());
        assertEquals(1, range[0].getDayOfMonth());
        assertEquals(12, range[1].getMonthValue());
        assertEquals(31, range[1].getDayOfMonth());
    }

    @Test
    void testGetXAxisLabels_Today() {
        List<String> labels = TimeRangeUtil.getXAxisLabels(TimeRangeEnum.TODAY);

        assertNotNull(labels);
        assertEquals(12, labels.size());
        assertEquals("0时", labels.get(0));
        assertEquals("22时", labels.get(11));
    }

    @Test
    void testGetXAxisLabels_Week() {
        List<String> labels = TimeRangeUtil.getXAxisLabels(TimeRangeEnum.WEEK);

        assertNotNull(labels);
        assertEquals(7, labels.size());
        assertEquals("周一", labels.get(0));
        assertEquals("周日", labels.get(6));
    }

    @Test
    void testGetXAxisLabels_Year() {
        List<String> labels = TimeRangeUtil.getXAxisLabels(TimeRangeEnum.YEAR);

        assertNotNull(labels);
        assertEquals(12, labels.size());
        assertEquals("1月", labels.get(0));
        assertEquals("12月", labels.get(11));
    }

    @Test
    void exclusiveRangeEndsAtNextDayStart() {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndExclusiveTime(
                TimeRangeEnum.CUSTOM, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 13));

        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2026, 8, 14, 0, 0), range[1]);
    }
}
