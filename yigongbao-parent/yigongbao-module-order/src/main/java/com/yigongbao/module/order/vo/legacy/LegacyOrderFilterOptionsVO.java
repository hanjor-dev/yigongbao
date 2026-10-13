package com.yigongbao.module.order.vo.legacy;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LegacyOrderFilterOptionsVO {
    private List<String> statuses;
    private List<String> businessTypes;
}
