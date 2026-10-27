package com.yigongbao.module.backup.manifest.model;

import java.time.Instant;

/** 清单确认结果；授权仅用于短期只读下载会话。 */
public record ManifestConfirmation(long orderId, String revision, String identityDigest,
                                   Instant readOnlyUntil) {
}
