package com.yigongbao.module.backup.manifest.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/** 可重复生成、可确认的订单业务快照和文件清单。 */
public record OrderManifest(
        long schemaVersion,
        long orderId,
        String revision,
        Instant generatedAt,
        Map<String, Object> order,
        List<ManifestAsset> assets,
        String canonicalJson,
        String identityDigest
) {
    public OrderManifest {
        order = Collections.unmodifiableMap(new LinkedHashMap<>(order == null ? Map.of() : order));
        assets = List.copyOf(assets == null ? List.of() : assets);
    }
}
