package com.yigongbao.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoVO {
    private Integer id;
    private String title;
    private Integer count;
    private String link;
    private Boolean urgent;
}
