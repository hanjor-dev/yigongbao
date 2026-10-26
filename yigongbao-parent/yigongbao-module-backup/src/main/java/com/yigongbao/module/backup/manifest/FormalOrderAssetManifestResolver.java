package com.yigongbao.module.backup.manifest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.backup.manifest.model.ManifestAsset;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignPackageFileScreenshotEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileScreenshotMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析正式订单当前所有文件关系。
 *
 * <p>业务关系表只保存文件 ID 或文件 URL，清单必须把它们解析到
 * {@code file_detail} 后再生成对象键、原始文件名、哈希和大小。任何关系指向的文件
 * 不存在时均失败，禁止生成不完整清单。</p>
 */
@Component
public class FormalOrderAssetManifestResolver implements ManifestAssetResolver {
    private static final Pattern SHA256_PATTERN = Pattern.compile(
            "\\\"(?:sha256|SHA256|sha-256|SHA-256)\\\"\\s*:\\s*\\\"([0-9a-fA-F]{64})\\\"");

    private final FileDetailMapper fileMapper;
    private final OrderFileMapper orderFileMapper;
    private final DesignPackageMapper packageMapper;
    private final DesignPackageFileMapper packageFileMapper;
    private final DesignPackageFileScreenshotMapper screenshotMapper;
    private final DesignModelMapper modelMapper;
    private final DesignInstructionMapper instructionMapper;
    private final DesignDrawingMapper drawingMapper;
    private final ProductionRecordMapper productionMapper;

    public FormalOrderAssetManifestResolver(FileDetailMapper fileMapper, OrderFileMapper orderFileMapper,
                                            DesignPackageMapper packageMapper,
                                            DesignPackageFileMapper packageFileMapper,
                                            DesignPackageFileScreenshotMapper screenshotMapper,
                                            DesignModelMapper modelMapper,
                                            DesignInstructionMapper instructionMapper,
                                            DesignDrawingMapper drawingMapper,
                                            ProductionRecordMapper productionMapper) {
        this.fileMapper = Objects.requireNonNull(fileMapper);
        this.orderFileMapper = Objects.requireNonNull(orderFileMapper);
        this.packageMapper = Objects.requireNonNull(packageMapper);
        this.packageFileMapper = Objects.requireNonNull(packageFileMapper);
        this.screenshotMapper = Objects.requireNonNull(screenshotMapper);
        this.modelMapper = Objects.requireNonNull(modelMapper);
        this.instructionMapper = Objects.requireNonNull(instructionMapper);
        this.drawingMapper = Objects.requireNonNull(drawingMapper);
        this.productionMapper = Objects.requireNonNull(productionMapper);
    }

    @Override
    public String assetType() {
        return "file";
    }

    @Override
    public List<ManifestAsset> resolve(OrderMainEntity order) {
        if (order == null || order.getId() == null) {
            throw new ManifestResolutionException("订单不能为空");
        }
        Long orderId = order.getId();
        List<ManifestAsset> assets = new ArrayList<>();
        List<DesignPackageEntity> packages = packageMapper.selectList(active(DesignPackageEntity.class)
                .eq(DesignPackageEntity::getOrderId, orderId)
                .eq(DesignPackageEntity::getIsDeleted, 0));
        List<Long> packageIds = packages.stream().map(DesignPackageEntity::getId)
                .filter(Objects::nonNull).toList();

        for (OrderFileEntity relation : orderFileMapper.selectList(active(OrderFileEntity.class)
                .eq(OrderFileEntity::getOrderId, orderId)
                .eq(OrderFileEntity::getIsDeleted, 0))) {
            assets.add(asset(relation.getFileId(), "ORDER_FILE", relation.getId(), relation.getFileCategory()));
        }
        for (DesignPackageEntity relation : packages) {
            assets.add(asset(relation.getFileId(), "DESIGN_PACKAGE", relation.getId(), relation.getPackageCode()));
        }

        List<DesignPackageFileEntity> packageFiles = packageIds.isEmpty() ? List.of()
                : packageFileMapper.selectList(active(DesignPackageFileEntity.class)
                .in(DesignPackageFileEntity::getPackageId, packageIds)
                .eq(DesignPackageFileEntity::getIsDeleted, 0));
        for (DesignPackageFileEntity relation : packageFiles) {
            assets.add(asset(relation.getFileId(), "DESIGN_PACKAGE_FILE", relation.getId(), relation.getFilePath()));
        }

        Set<Long> packageFileIds = packageFiles.stream().map(DesignPackageFileEntity::getId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (!packageFileIds.isEmpty()) {
            for (DesignPackageFileScreenshotEntity relation : screenshotMapper.selectList(
                    active(DesignPackageFileScreenshotEntity.class).in(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileIds)
                            .eq(DesignPackageFileScreenshotEntity::getIsDeleted, 0))) {
                assets.add(asset(relation.getFileId(), "DESIGN_SCREENSHOT", relation.getId(), null));
            }
        }

        for (DesignModelEntity relation : modelMapper.selectList(active(DesignModelEntity.class)
                .eq(DesignModelEntity::getOrderId, orderId)
                .eq(DesignModelEntity::getIsDeleted, 0))) {
            assets.add(asset(relation.getFileId(), "DESIGN_MODEL", relation.getId(), null));
        }
        for (DesignInstructionEntity relation : instructionMapper.selectList(active(DesignInstructionEntity.class)
                .eq(DesignInstructionEntity::getOrderId, orderId)
                .eq(DesignInstructionEntity::getIsDeleted, 0))) {
            addIfPresent(assets, relation.getTemplateFileId(), "DESIGN_INSTRUCTION_TEMPLATE", relation.getId(), null);
            addIfPresent(assets, relation.getRevisedFileId(), "DESIGN_INSTRUCTION_REVISED", relation.getId(), null);
        }
        for (DesignDrawingEntity relation : drawingMapper.selectList(active(DesignDrawingEntity.class)
                .eq(DesignDrawingEntity::getOrderId, orderId)
                .eq(DesignDrawingEntity::getIsDeleted, 0))) {
            addIfPresent(assets, relation.getTemplateFileId(), "DESIGN_DRAWING_TEMPLATE", relation.getId(), null);
            addIfPresent(assets, relation.getQrFileId(), "DESIGN_DRAWING_QR", relation.getId(), null);
            addIfPresent(assets, relation.getRevisedFileId(), "DESIGN_DRAWING_REVISED", relation.getId(), null);
        }
        for (ProductionRecordEntity relation : productionMapper.selectList(active(ProductionRecordEntity.class)
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .eq(ProductionRecordEntity::getIsDeleted, 0))) {
            if (!hasText(relation.getFlowCardFileUrl())) {
                continue;
            }
            FileDetail detail = fileMapper.selectOne(new LambdaQueryWrapper<FileDetail>()
                    .eq(FileDetail::getUrl, relation.getFlowCardFileUrl())
                    .eq(FileDetail::getIsDeleted, 0));
            if (detail == null) {
                throw new ManifestResolutionException("找不到流转卡文件: " + relation.getFlowCardFileUrl());
            }
            assets.add(asset(detail, "FLOW_CARD", relation.getId(), null));
        }
        return assets;
    }

    private ManifestAsset asset(String fileId, String relationKind, Long relationId, String relationValue) {
        if (!hasText(fileId)) {
            throw new ManifestResolutionException("订单文件关系缺少文件 ID: " + relationKind + ":" + relationId);
        }
        FileDetail detail = fileMapper.selectById(fileId);
        if (detail == null) {
            throw new ManifestResolutionException("订单文件不存在: " + fileId);
        }
        return asset(detail, relationKind, relationId, relationValue);
    }

    private void addIfPresent(List<ManifestAsset> assets, String fileId, String relationKind,
                              Long relationId, String relationValue) {
        if (hasText(fileId)) {
            assets.add(asset(fileId, relationKind, relationId, relationValue));
        }
    }

    private ManifestAsset asset(FileDetail detail, String relationKind, Long relationId, String relationValue) {
        if (detail == null || !hasText(detail.getId())) {
            throw new ManifestResolutionException("文件明细不存在");
        }
        if (detail.getSize() == null || detail.getSize() < 0) {
            throw new ManifestResolutionException("文件大小缺失: " + detail.getId());
        }
        String objectKey = objectKey(detail);
        if (!hasText(objectKey)) {
            throw new ManifestResolutionException("文件对象键缺失: " + detail.getId());
        }
        String originalFilename = hasText(detail.getOriginalFilename()) ? detail.getOriginalFilename() : detail.getFilename();
        if (!hasText(originalFilename)) {
            throw new ManifestResolutionException("文件原始名称缺失: " + detail.getId());
        }
        String relationIdText = relationId == null ? "0" : relationId.toString();
        String relationHash = sha256(relationKind + "|" + relationIdText + "|" + detail.getId()
                + "|" + (relationValue == null ? "" : relationValue));
        Map<String, Object> relation = Map.of(
                "relationKind", relationKind,
                "sourceRecordId", relationIdText,
                "relationVersion", relationVersion(detail),
                "relationHash", relationHash,
                "originalFileId", detail.getId(),
                "originalUrl", hasText(detail.getUrl()) ? detail.getUrl() : objectKey);
        return new ManifestAsset(detail.getId(), "file", objectKey, originalFilename,
                sha256FromHashInfo(detail.getHashInfo()), detail.getSize(), List.of(relation));
    }

    private static <T> LambdaQueryWrapper<T> active(Class<T> type) {
        return new LambdaQueryWrapper<T>();
    }

    private static String relationVersion(FileDetail detail) {
        if (detail.getUpdateTime() != null) {
            return detail.getUpdateTime().toString();
        }
        if (detail.getCreateTime() != null) {
            return detail.getCreateTime().toString();
        }
        return "0";
    }

    private static String objectKey(FileDetail detail) {
        String key = String.join("", text(detail.getBasePath()), text(detail.getPath()), text(detail.getFilename()))
                .replace('\\', '/');
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return hasText(key) ? key : detail.getUrl();
    }

    private static String sha256FromHashInfo(String hashInfo) {
        if (!hasText(hashInfo)) {
            return null;
        }
        Matcher matcher = SHA256_PATTERN.matcher(hashInfo);
        return matcher.find() ? matcher.group(1).toLowerCase() : null;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }
}
