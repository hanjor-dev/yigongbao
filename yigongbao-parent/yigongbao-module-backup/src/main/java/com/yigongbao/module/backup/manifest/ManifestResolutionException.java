package com.yigongbao.module.backup.manifest;

/** 清单资产关系无法解释或清单确认不一致时的业务异常。 */
public class ManifestResolutionException extends RuntimeException {
    public ManifestResolutionException(String message) {
        super(message);
    }
}
