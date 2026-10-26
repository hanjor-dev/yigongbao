package com.yigongbao.module.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线上备份协作模块配置。
 *
 * <p>COS 实际删除属于危险能力，默认值必须保持关闭；只有后续安全闸门全部完成后才能由生产配置显式开启。</p>
 */
@ConfigurationProperties(prefix = "yigongbao.backup")
public class BackupProperties {

    /** 是否允许执行 COS 实际删除，默认关闭。 */
    private boolean cosDeletionEnabled;

    /** 内部机器接口安全配置。 */
    private final Security security = new Security();

    public boolean isCosDeletionEnabled() {
        return cosDeletionEnabled;
    }

    public void setCosDeletionEnabled(boolean cosDeletionEnabled) {
        this.cosDeletionEnabled = cosDeletionEnabled;
    }

    public Security getSecurity() {
        return security;
    }

    /** 内部机器接口安全配置。 */
    public static class Security {

        /**
         * 仅供 test profile 的上下文测试旁路未实现的验证器。
         * 非 test profile 即使误配置为 true 也不会放行请求。
         */
        private boolean allowUnverifiedRequestsForTests;

        public boolean isAllowUnverifiedRequestsForTests() {
            return allowUnverifiedRequestsForTests;
        }

        public void setAllowUnverifiedRequestsForTests(boolean allowUnverifiedRequestsForTests) {
            this.allowUnverifiedRequestsForTests = allowUnverifiedRequestsForTests;
        }
    }
}
