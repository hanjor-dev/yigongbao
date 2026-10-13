package com.yigongbao.module.production.helper;

import com.yigongbao.module.production.enums.ProcessTypeEnum;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 统一计算打印完成后的清洗、固化和清洗干燥排程。 */
public final class PostProcessingScheduleCalculator {

    private static final int WASH_START_INTERVAL_MINUTES = 2;
    private static final int WASH_DURATION_MINUTES = 10;
    private static final int CURE_START_INTERVAL_MINUTES = 1;
    private static final int CURE_DURATION_MINUTES = 40;
    private static final int CLEAN_DRY_START_INTERVAL_MINUTES = 1;
    private static final int CLEAN_DRY_DURATION_MINUTES = 10;

    private PostProcessingScheduleCalculator() {
    }

    public static Map<String, TimeRange> calculate(LocalDateTime printFinishTime) {
        LocalDateTime printEnd = Objects.requireNonNull(printFinishTime, "printFinishTime")
                .withNano(0);
        LocalDateTime washStart = printEnd.plusMinutes(WASH_START_INTERVAL_MINUTES);
        LocalDateTime washEnd = washStart.plusMinutes(WASH_DURATION_MINUTES);
        LocalDateTime cureStart = washEnd.plusMinutes(CURE_START_INTERVAL_MINUTES);
        LocalDateTime cureEnd = cureStart.plusMinutes(CURE_DURATION_MINUTES);
        LocalDateTime cleanDryStart = cureEnd.plusMinutes(CLEAN_DRY_START_INTERVAL_MINUTES);
        LocalDateTime cleanDryEnd = cleanDryStart.plusMinutes(CLEAN_DRY_DURATION_MINUTES);

        Map<String, TimeRange> schedule = new LinkedHashMap<>();
        schedule.put(ProcessTypeEnum.WASH.getCode(), new TimeRange(washStart, washEnd));
        schedule.put(ProcessTypeEnum.CURE.getCode(), new TimeRange(cureStart, cureEnd));
        schedule.put(ProcessTypeEnum.CLEAN_DRY.getCode(), new TimeRange(cleanDryStart, cleanDryEnd));
        return schedule;
    }

    public record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
