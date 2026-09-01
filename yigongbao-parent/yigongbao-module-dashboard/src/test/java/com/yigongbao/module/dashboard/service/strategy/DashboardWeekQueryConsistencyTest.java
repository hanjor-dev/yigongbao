package com.yigongbao.module.dashboard.service.strategy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardWeekQueryConsistencyTest {

    @Test
    void allDashboardStrategiesUseMondayBasedWeekdayBuckets() throws IOException {
        Path strategyDirectory = Path.of("src/main/java/com/yigongbao/module/dashboard/service/strategy");

        try (Stream<Path> paths = Files.list(strategyDirectory)) {
            paths.filter(path -> path.getFileName().toString().endsWith("DashboardStrategy.java"))
                    .filter(path -> !path.getFileName().toString().equals("DashboardStrategy.java"))
                    .forEach(path -> {
                        String source;
                        try {
                            source = Files.readString(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (source.contains("case WEEK") || source.contains("TimeRangeEnum.WEEK")) {
                            assertThat(source)
                                    .as("策略 %s 不应使用周日优先的 DAYOFWEEK", path.getFileName())
                                    .doesNotContain("DAYOFWEEK(");
                            assertThat(source)
                                    .as("策略 %s 的周查询应使用 Monday=0 的 WEEKDAY", path.getFileName())
                                    .contains("WEEKDAY(");
                            assertThat(source)
                                    .as("策略 %s 不应再次对 WEEKDAY 结果减一", path.getFileName())
                                    .doesNotContain("row.get(\"weekday\")).intValue() - 1");
                        }
                    });
        }
    }
}
