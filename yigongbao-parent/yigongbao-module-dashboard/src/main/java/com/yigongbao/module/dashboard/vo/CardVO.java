package com.yigongbao.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardVO {
    private String key;
    private String title;
    private Object value;
    private String unit;
    private String change;
    private String trend;
    private String link;
}
