package com.yigongbao.module.backup.candidate.model;

/** 候选对账结果。 */
public record CandidateReconcileResult(int scanned, int repaired, int cancelled, int deleted) {
}
