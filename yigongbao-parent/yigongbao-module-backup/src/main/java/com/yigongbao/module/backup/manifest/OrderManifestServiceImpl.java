package com.yigongbao.module.backup.manifest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.backup.manifest.model.ManifestAsset;
import com.yigongbao.module.backup.manifest.model.ManifestConfirmation;
import com.yigongbao.module.backup.manifest.model.OrderManifest;
import com.yigongbao.module.order.service.OrderMainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 默认清单实现：全字段订单快照、typed resolver 聚合和确定性 JCS 风格摘要。 */
@Service
@Slf4j
public class OrderManifestServiceImpl implements OrderManifestService {

    private static final long SCHEMA_VERSION = 1L;
    private static final Duration READ_ONLY_TTL = Duration.ofMinutes(5);

    private final OrderMainService orderService;
    private final List<ManifestAssetResolver> resolvers;
    private final ObjectMapper canonicalMapper;

    public OrderManifestServiceImpl(OrderMainService orderService,
                                    List<ManifestAssetResolver> resolvers,
                                    ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.resolvers = resolvers == null ? List.of() : List.copyOf(resolvers);
        this.canonicalMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.ALWAYS)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public OrderManifest createManifest(Long orderId) {
        if (orderId == null) {
            throw new ManifestResolutionException("订单 ID 不能为空");
        }
        OrderMainEntity order = orderService.getById(orderId);
        assertEligible(order);
        Map<String, Object> orderSnapshot = snapshotAllBusinessFields(order);
        Map<String, ManifestAsset> deduplicated = new LinkedHashMap<>();
        for (ManifestAssetResolver resolver : resolvers) {
            List<ManifestAsset> resolved;
            try {
                resolved = resolver.resolve(order);
            } catch (RuntimeException ex) {
                log.warn("订单清单资产解析失败: orderId={}, assetType={}", orderId, resolver.assetType());
                throw new ManifestResolutionException("资产关系解析失败: " + resolver.assetType());
            }
            if (resolved == null) {
                throw new ManifestResolutionException("资产解析器返回空结果: " + resolver.assetType());
            }
            for (ManifestAsset asset : resolved) {
                if (asset == null) {
                    throw new ManifestResolutionException("资产解析器返回空资产: " + resolver.assetType());
                }
                ManifestAsset previous = deduplicated.putIfAbsent(asset.assetId(), asset);
                if (previous != null && (!previous.objectKey().equals(asset.objectKey())
                        || !previous.assetType().equals(asset.assetType()))) {
                    throw new ManifestResolutionException("同一资产存在冲突关系: " + asset.assetId());
                }
            }
        }
        List<ManifestAsset> assets = new ArrayList<>(deduplicated.values());
        assets.sort(Comparator.comparing(ManifestAsset::assetId));
        String canonicalJson = canonicalJson(orderId, orderSnapshot, assets);
        String digest = sha256(canonicalJson);
        return new OrderManifest(SCHEMA_VERSION, orderId, digest, Instant.now(),
                orderSnapshot, assets, canonicalJson, digest);
    }

    @Override
    public ManifestConfirmation confirmManifest(Long orderId, String revision, String identityDigest) {
        if (revision == null || identityDigest == null || !MessageDigest.isEqual(
                revision.getBytes(StandardCharsets.UTF_8), identityDigest.getBytes(StandardCharsets.UTF_8))) {
            throw new ManifestResolutionException("清单确认摘要不一致");
        }
        OrderManifest manifest = createManifest(orderId);
        if (!MessageDigest.isEqual(manifest.revision().getBytes(StandardCharsets.UTF_8),
                revision.getBytes(StandardCharsets.UTF_8))) {
            throw new ManifestResolutionException("清单在下载确认前已发生变化");
        }
        return new ManifestConfirmation(orderId, revision, identityDigest,
                Instant.now().plus(READ_ONLY_TTL));
    }

    private void assertEligible(OrderMainEntity order) {
        if (order == null || (order.getIsDeleted() != null && order.getIsDeleted() == 1)
                || FlowStatusEnum.DRAFT.getValue().equals(order.getStatus())
                || FlowStatusEnum.CANCELLED.getValue().equals(order.getStatus())) {
            throw new ManifestResolutionException("订单不是可备份的正式订单");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> snapshotAllBusinessFields(OrderMainEntity order) {
        Map<String, Object> values = canonicalMapper.convertValue(order, Map.class);
        return new LinkedHashMap<>(values);
    }

    private String canonicalJson(Long orderId, Map<String, Object> order, List<ManifestAsset> assets) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assets", assets);
        payload.put("order", order);
        payload.put("orderId", orderId);
        payload.put("schemaVersion", SCHEMA_VERSION);
        try {
            return canonicalMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ManifestResolutionException("清单规范化失败");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }
}
