package com.yigongbao.module.notification.vo;

import lombok.Data;

import java.util.Map;

/**
 * 未读消息数量 VO
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
public class UnreadCountVO {
    private long total;
    private Map<String, Long> byCategory;
}
