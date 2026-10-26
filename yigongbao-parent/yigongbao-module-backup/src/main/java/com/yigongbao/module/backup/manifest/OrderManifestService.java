package com.yigongbao.module.backup.manifest;

import com.yigongbao.module.backup.manifest.model.ManifestConfirmation;
import com.yigongbao.module.backup.manifest.model.OrderManifest;

/** 订单业务快照和完整文件清单服务。 */
public interface OrderManifestService {
    OrderManifest createManifest(Long orderId);

    ManifestConfirmation confirmManifest(Long orderId, String revision, String identityDigest);
}
