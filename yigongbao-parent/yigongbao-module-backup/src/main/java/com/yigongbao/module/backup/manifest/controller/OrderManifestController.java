package com.yigongbao.module.backup.manifest.controller;

import com.yigongbao.module.backup.common.response.InternalApiResponse;
import com.yigongbao.module.backup.manifest.OrderManifestService;
import com.yigongbao.module.backup.manifest.model.ManifestConfirmation;
import com.yigongbao.module.backup.manifest.model.OrderManifest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 线上订单清单接口；向本地备份系统暴露完整订单业务快照和文件资产关系。 */
@RestController
@RequestMapping("/internal/backup/v1/orders")
public class OrderManifestController {
    private final OrderManifestService service;

    public OrderManifestController(OrderManifestService service) {
        this.service = service;
    }

    /** 生成当前正式订单的只读清单，清单摘要作为恢复和备份幂等依据。 */
    @GetMapping("/{orderId}/manifest")
    public ResponseEntity<InternalApiResponse<ManifestResponse>> get(@PathVariable Long orderId) {
        return ResponseEntity.ok(InternalApiResponse.success(toResponse(service.createManifest(orderId)), "backup-manifest"));
    }

    /** 确认本地已按同一清单完成下载，防止线上清单在确认窗口内发生变化。 */
    @PostMapping("/{orderId}/manifest/{revision}/confirm")
    public ResponseEntity<InternalApiResponse<ManifestConfirmation>> confirm(
            @PathVariable Long orderId,
            @PathVariable String revision,
            @RequestBody ConfirmCommand command) {
        if (command == null || command.manifestJcsSha256() == null || command.objectIdentityDigest() == null) {
            throw new IllegalArgumentException("清单确认摘要不能为空");
        }
        if (!revision.equals(command.manifestJcsSha256())) {
            throw new IllegalArgumentException("清单版本与摘要不一致");
        }
        return ResponseEntity.ok(InternalApiResponse.success(
                service.confirmManifest(orderId, revision, command.manifestJcsSha256()), "backup-manifest-confirm"));
    }

    private static ManifestResponse toResponse(OrderManifest manifest) {
        List<AssetResponse> assets = manifest.assets().stream().map(asset -> new AssetResponse(
                asset.assetId(), null, asset.assetType(), asset.originalFilename(), asset.size(),
                asset.contentSha256(), new SourceObject("COS", "", asset.objectKey(), "*", asset.objectKey()),
                asset.relations())).toList();
        return new ManifestResponse(manifest.revision(), manifest.identityDigest(), manifest.orderId(),
                manifest.order(), assets, Map.of());
    }

    public record ConfirmCommand(String manifestJcsSha256, String objectIdentityDigest) {}

    public record ManifestResponse(String manifestRevision, String manifestJcsSha256, long orderId,
                                   Map<String, Object> order, List<AssetResponse> assets,
                                   Map<String, Object> downloadGrant) {}

    public record AssetResponse(String assetIdentity, Long sourceFileId, String assetType,
                                String originalFilename, long size, String sha256,
                                SourceObject sourceObject, List<Map<String, Object>> relations) {}

    public record SourceObject(String platform, String bucket, String objectKey,
                               String versionIdentity, String url) {}
}
