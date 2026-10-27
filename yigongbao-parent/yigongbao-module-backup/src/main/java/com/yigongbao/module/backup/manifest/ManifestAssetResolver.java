package com.yigongbao.module.backup.manifest;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.backup.manifest.model.ManifestAsset;

import java.util.List;

/** 按文件资产类型解析订单关联；任何无法解释的关系都必须抛出异常。 */
public interface ManifestAssetResolver {
    String assetType();

    List<ManifestAsset> resolve(OrderMainEntity order);
}
