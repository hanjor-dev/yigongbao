package com.yigongbao.module.backup.candidate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 线上订单备份候选生命周期事实。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_backup_candidate")
public class OrderBackupCandidateEntity extends BaseEntity {

    private Long eventId;
    private Long orderId;
    private Integer generation;
    private LocalDateTime eligibleAt;
    private LocalDateTime orderCreatedAt;
    private String eventStatus;
}
