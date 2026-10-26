package com.yigongbao.module.backup.candidate.model;

/** 候选事件状态。取消/删除事件保留事实，避免把已发现订单静默丢失。 */
public enum CandidateEventStatus {
    ACTIVE,
    CANCELLED,
    DELETED
}
