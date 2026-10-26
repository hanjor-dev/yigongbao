package com.yigongbao.module.backup.candidate.model;

import java.time.LocalDateTime;

/** 候选事件分页查询条件。 */
public record CandidateSearchQuery(String cursor, int pageSize, String highWatermark,
                                   LocalDateTime eligibleFrom, LocalDateTime eligibleTo) {
    public CandidateSearchQuery {
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("pageSize 必须在 1 到 500 之间");
        }
    }
}
