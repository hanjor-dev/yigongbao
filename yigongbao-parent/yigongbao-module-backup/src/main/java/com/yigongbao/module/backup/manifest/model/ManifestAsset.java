package com.yigongbao.module.backup.manifest.model;

import java.util.Map;

/** 订单清单中的单个文件资产及其业务关联。 */
public record ManifestAsset(
        String assetId,
        String assetType,
        String objectKey,
        String contentSha256,
        long size,
        Map<String, Object> relation
) {
    public ManifestAsset {
        if (assetId == null || assetId.isBlank() || assetType == null || assetType.isBlank()
                || objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("清单资产标识、类型和对象键不能为空");
        }
        relation = relation == null ? Map.of() : Map.copyOf(relation);
    }
}
