package com.yigongbao.module.backup.security.model;

/** 内部机器请求的最小权限域。 */
public enum MachineScope {
    MANIFEST,
    ACCOUNT,
    RESTORE,
    CLEANUP,
    CLEANUP_PERMIT
}
