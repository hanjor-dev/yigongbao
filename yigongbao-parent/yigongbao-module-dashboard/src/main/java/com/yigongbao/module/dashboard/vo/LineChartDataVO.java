package com.yigongbao.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineChartDataVO {
    private List<String> xAxis;
    private List<SeriesVO> series;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesVO {
        private String name;
        private List<Integer> data;
    }
}
