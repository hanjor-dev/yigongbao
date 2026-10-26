package com.yigongbao.module.backup.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.backup.manifest.model.ManifestAsset;
import com.yigongbao.module.backup.manifest.model.OrderManifest;
import com.yigongbao.module.order.service.OrderMainService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderManifestServiceImplTest {
    private final OrderMainService orderService = mock(OrderMainService.class);
    private final ManifestAssetResolver resolver = mock(ManifestAssetResolver.class);

    @Test
    void createsDeterministicFullSnapshotAndDeduplicatesAssets() {
        OrderMainEntity order = formalOrder();
        when(orderService.getById(7L)).thenReturn(order);
        when(resolver.assetType()).thenReturn("attachment");
        ManifestAsset asset = new ManifestAsset("f-1", "attachment", "orders/7/a.bin", "abc", 3,
                Map.of("relationId", 11L));
        when(resolver.resolve(order)).thenReturn(List.of(asset, asset));
        var service = new OrderManifestServiceImpl(orderService, List.of(resolver), new ObjectMapper());

        OrderManifest first = service.createManifest(7L);
        OrderManifest second = service.createManifest(7L);

        assertThat(first.assets()).hasSize(1);
        assertThat(first.canonicalJson()).isEqualTo(second.canonicalJson());
        assertThat(first.identityDigest()).isEqualTo(second.identityDigest());
        assertThat(first.order()).containsKeys("id", "status", "createTime");
        assertThat(first.assets().get(0).relations()).hasSize(1);
    }

    @Test
    void refusesToCreateManifestWhenNoAssetResolverIsRegistered() {
        when(orderService.getById(7L)).thenReturn(formalOrder());
        var service = new OrderManifestServiceImpl(orderService, List.of(), new ObjectMapper());

        assertThatThrownBy(() -> service.createManifest(7L))
                .isInstanceOf(ManifestResolutionException.class)
                .hasMessageContaining("解析器");
    }

    @Test
    void preservesDistinctBusinessRelationsWhenObjectIsDeduplicated() {
        OrderMainEntity order = formalOrder();
        when(orderService.getById(7L)).thenReturn(order);
        when(resolver.assetType()).thenReturn("attachment");
        ManifestAsset first = new ManifestAsset("f-1", "attachment", "orders/7/a.bin", "abc", 3,
                Map.of("relationId", 11L));
        ManifestAsset second = new ManifestAsset("f-1", "attachment", "orders/7/a.bin", "abc", 3,
                Map.of("relationId", 12L));
        when(resolver.resolve(order)).thenReturn(List.of(first, second));

        var manifest = new OrderManifestServiceImpl(orderService, List.of(resolver), new ObjectMapper())
                .createManifest(7L);

        assertThat(manifest.assets()).hasSize(1);
        assertThat(manifest.assets().getFirst().relations()).hasSize(2);
    }

    @Test
    void resolverFailureFailsWholeOrder() {
        OrderMainEntity order = formalOrder();
        when(orderService.getById(7L)).thenReturn(order);
        when(resolver.assetType()).thenReturn("drawing");
        when(resolver.resolve(order)).thenThrow(new IllegalStateException("unreadable relation"));
        var service = new OrderManifestServiceImpl(orderService, List.of(resolver), new ObjectMapper());

        assertThatThrownBy(() -> service.createManifest(7L))
                .isInstanceOf(ManifestResolutionException.class)
                .hasMessageContaining("drawing");
    }

    @Test
    void draftOrderCannotBeManifested() {
        OrderMainEntity order = formalOrder();
        order.setStatus(com.yigongbao.flow.enums.FlowStatusEnum.DRAFT.getValue());
        when(orderService.getById(7L)).thenReturn(order);
        var service = new OrderManifestServiceImpl(orderService, List.of(), new ObjectMapper());

        assertThatThrownBy(() -> service.createManifest(7L))
                .isInstanceOf(ManifestResolutionException.class);
    }

    private static OrderMainEntity formalOrder() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(7L);
        order.setStatus(2030);
        order.setIsDeleted(0);
        order.setOrderCode("ORD-7");
        order.setCreateTime(java.time.LocalDateTime.of(2026, 1, 2, 3, 4));
        return order;
    }
}
