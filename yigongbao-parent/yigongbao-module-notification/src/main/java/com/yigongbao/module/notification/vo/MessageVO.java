package com.yigongbao.module.notification.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息列表 VO
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
public class MessageVO {
    private Long id;
    private String messageType;
    private String category;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private String bizData;
    private String bizStatus;
    private String jumpUrl;
    private Integer isRead;
    private Integer isConfirmed;
    private LocalDateTime createTime;
}
