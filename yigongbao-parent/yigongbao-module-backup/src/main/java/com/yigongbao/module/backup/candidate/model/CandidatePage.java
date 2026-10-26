package com.yigongbao.module.backup.candidate.model;

import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;

import java.util.List;

/** 候选事件页，highWatermark 固定当前查询窗口，防止分页期间漏读新事件。 */
public record CandidatePage(List<OrderBackupCandidateEntity> items, String nextCursor,
                            boolean hasMore, String highWatermark) {
}
