package com.yigongbao.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知消息实体
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification_message")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageEntity extends BaseEntity {

    @TableField("message_type")
    private String messageType;

    @TableField("category")
    private String category;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("biz_data")
    private String bizData;

    @TableField("biz_status")
    private String bizStatus;

    @TableField("jump_url")
    private String jumpUrl;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("is_confirmed")
    private Integer isConfirmed;

    @TableField("confirmed_time")
    private LocalDateTime confirmedTime;
}
