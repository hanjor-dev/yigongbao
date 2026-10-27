package com.yigongbao.module.backup.manifest;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormalOrderAssetManifestResolverTest {
    private final FileDetailMapper fileMapper = mock(FileDetailMapper.class);
    private final OrderFileMapper orderFileMapper = mock(OrderFileMapper.class);
    private final DesignPackageMapper packageMapper = mock(DesignPackageMapper.class);
    private final DesignPackageFileMapper packageFileMapper = mock(DesignPackageFileMapper.class);
    private final DesignPackageFileScreenshotMapper screenshotMapper = mock(DesignPackageFileScreenshotMapper.class);
    private final DesignModelMapper modelMapper = mock(DesignModelMapper.class);
    private final DesignInstructionMapper instructionMapper = mock(DesignInstructionMapper.class);
    private final DesignDrawingMapper drawingMapper = mock(DesignDrawingMapper.class);
    private final ProductionRecordMapper productionMapper = mock(ProductionRecordMapper.class);
    private FormalOrderAssetManifestResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FormalOrderAssetManifestResolver(fileMapper, orderFileMapper, packageMapper,
                packageFileMapper, screenshotMapper, modelMapper, instructionMapper, drawingMapper, productionMapper);
    }

    @Test
    void resolvesAllFormalOrderRelationsAndPreservesFileMetadata() {
        OrderMainEntity order = order();
        OrderFileEntity direct = orderFile(100L, "f-direct", "10.1");
        DesignPackageEntity pack = pack(200L, "f-package");
        DesignPackageFileEntity packageFile = packageFile(300L, 200L, "f-inner");
        DesignPackageFileScreenshotEntity screenshot = screenshot(400L, 300L, "f-shot");
        DesignModelEntity model = model(500L, "f-model");
        DesignInstructionEntity instruction = instruction(600L, 200L, "f-template", "f-revised");
        DesignDrawingEntity drawing = drawing(700L, 200L, "f-drawing-template", "f-qr", "f-drawing-revised");
        ProductionRecordEntity flowCard = flowCard(800L, "https://cos.example/flow-card.xlsx");

        when(orderFileMapper.selectList(any())).thenReturn(List.of(direct));
        when(packageMapper.selectList(any())).thenReturn(List.of(pack));
        when(packageFileMapper.selectList(any())).thenReturn(List.of(packageFile));
        when(screenshotMapper.selectList(any())).thenReturn(List.of(screenshot));
        when(modelMapper.selectList(any())).thenReturn(List.of(model));
        when(instructionMapper.selectList(any())).thenReturn(List.of(instruction));
        when(drawingMapper.selectList(any())).thenReturn(List.of(drawing));
        when(productionMapper.selectList(any())).thenReturn(List.of(flowCard));
        when(fileMapper.selectById(any())).thenAnswer(invocation -> file(invocation.getArgument(0, String.class)));
        when(fileMapper.selectOne(any())).thenReturn(file("f-flow"));

        List<ManifestAsset> assets = resolver.resolve(order);

        assertThat(assets).hasSize(11);
        assertThat(assets).extracting(ManifestAsset::assetId)
                .containsExactlyInAnyOrder("f-direct", "f-package", "f-inner", "f-shot", "f-model",
                        "f-template", "f-revised", "f-drawing-template", "f-qr", "f-drawing-revised", "f-flow");
        ManifestAsset directAsset = assets.stream().filter(item -> item.assetId().equals("f-direct")).findFirst().orElseThrow();
        assertThat(directAsset.originalFilename()).isEqualTo("f-direct.dcm");
        assertThat(directAsset.objectKey()).isEqualTo("orders/7/f-direct.dcm");
        assertThat(directAsset.size()).isEqualTo(1024L);
        assertThat(directAsset.relations()).singleElement().satisfies(relation -> {
            assertThat(relation).containsEntry("relationKind", "ORDER_FILE");
            assertThat(relation).containsEntry("originalFileId", "f-direct");
        });
    }

    @Test
    void missingReferencedFileFailsClosedInsteadOfCreatingPartialManifest() {
        when(orderFileMapper.selectList(any())).thenReturn(List.of(orderFile(100L, "missing", "10.1")));
        when(packageMapper.selectList(any())).thenReturn(List.of());
        when(modelMapper.selectList(any())).thenReturn(List.of());
        when(productionMapper.selectList(any())).thenReturn(List.of());
        when(fileMapper.selectById("missing")).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(order()))
                .isInstanceOf(ManifestResolutionException.class)
                .hasMessageContaining("missing");
    }

    private static OrderMainEntity order() {
        OrderMainEntity entity = new OrderMainEntity();
        entity.setId(7L);
        entity.setOrderCode("ORD-7");
        return entity;
    }

    private static OrderFileEntity orderFile(Long id, String fileId, String category) {
        OrderFileEntity entity = new OrderFileEntity();
        entity.setId(id);
        entity.setOrderId(7L);
        entity.setFileId(fileId);
        entity.setFileCategory(category);
        return entity;
    }

    private static DesignPackageEntity pack(Long id, String fileId) {
        DesignPackageEntity entity = new DesignPackageEntity();
        entity.setId(id);
        entity.setOrderId(7L);
        entity.setFileId(fileId);
        return entity;
    }

    private static DesignPackageFileEntity packageFile(Long id, Long packageId, String fileId) {
        DesignPackageFileEntity entity = new DesignPackageFileEntity();
        entity.setId(id);
        entity.setPackageId(packageId);
        entity.setFileId(fileId);
        return entity;
    }

    private static DesignPackageFileScreenshotEntity screenshot(Long id, Long packageFileId, String fileId) {
        DesignPackageFileScreenshotEntity entity = new DesignPackageFileScreenshotEntity();
        entity.setId(id);
        entity.setPackageFileId(packageFileId);
        entity.setFileId(fileId);
        return entity;
    }

    private static DesignModelEntity model(Long id, String fileId) {
        DesignModelEntity entity = new DesignModelEntity();
        entity.setId(id);
        entity.setOrderId(7L);
        entity.setFileId(fileId);
        return entity;
    }

    private static DesignInstructionEntity instruction(Long id, Long packageId, String template, String revised) {
        DesignInstructionEntity entity = new DesignInstructionEntity();
        entity.setId(id);
        entity.setPackageId(packageId);
        entity.setTemplateFileId(template);
        entity.setRevisedFileId(revised);
        return entity;
    }

    private static DesignDrawingEntity drawing(Long id, Long packageId, String template, String qr, String revised) {
        DesignDrawingEntity entity = new DesignDrawingEntity();
        entity.setId(id);
        entity.setPackageId(packageId);
        entity.setTemplateFileId(template);
        entity.setQrFileId(qr);
        entity.setRevisedFileId(revised);
        return entity;
    }

    private static ProductionRecordEntity flowCard(Long id, String url) {
        ProductionRecordEntity entity = new ProductionRecordEntity();
        entity.setId(id);
        entity.setOrderId(7L);
        entity.setFlowCardFileUrl(url);
        return entity;
    }

    private static FileDetail file(String id) {
        FileDetail detail = new FileDetail();
        detail.setId(id);
        detail.setUrl(id.equals("f-flow") ? "https://cos.example/flow-card.xlsx" : "https://cos.example/" + id);
        detail.setBasePath("orders/");
        detail.setPath("7/");
        detail.setFilename(id + ".dcm");
        detail.setOriginalFilename(id + ".dcm");
        detail.setSize(1024L);
        detail.setPlatform("cos");
        detail.setHashInfo("{\"sha256\":\"" + "a".repeat(64) + "\"}");
        detail.setIsDeleted(0);
        return detail;
    }
}
