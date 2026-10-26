package com.yigongbao.module.backup.manifest.model;

import java.util.Map;
import java.util.List;

/** 订单清单中的单个文件资产及其业务关联。 */
public record ManifestAsset(
        String assetId,
        String assetType,
        String objectKey,
        String contentSha256,
        long size,
        List<Map<String, Object>> relations
) {
    /** 兼容单关系 resolver；同一对象的多条业务关系由服务聚合到 relations。 */
    public ManifestAsset(String assetId, String assetType, String objectKey, String contentSha256,
                         long size, Map<String, Object> relation) {
        this(assetId, assetType, objectKey, contentSha256, size,
                relation == null ? List.of() : List.of(Map.copyOf(relation)));
    }

    public ManifestAsset {
        if (assetId == null || assetId.isBlank() || assetType == null || assetType.isBlank()
                || objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("清单资产标识、类型和对象键不能为空");
        }
        relations = relations == null ? List.of() : relations.stream()
                .map(item -> item == null ? Map.<String, Object>of() : Map.copyOf(item))
                .distinct()
                .toList();
    }
}
