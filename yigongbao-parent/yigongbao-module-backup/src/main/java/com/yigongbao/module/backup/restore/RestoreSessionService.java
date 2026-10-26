package com.yigongbao.module.backup.restore;

import java.util.List;

public interface RestoreSessionService {
    RestoreSession prepare(Long orderId, String manifestRevision, String relationDigest);
    RestoreSession get(String sessionId);
    RestoreSession commit(String sessionId, List<UploadedAsset> uploaded);
    RestoreSession abort(String sessionId, String reason);
    record UploadedAsset(String assetIdentity, String objectKey, String sha256, long size) {}
    record RestoreSession(String id, Long orderId, String status, String manifestRevision, String relationDigest, List<UploadedAsset> uploaded, String failureReason) {}
}
