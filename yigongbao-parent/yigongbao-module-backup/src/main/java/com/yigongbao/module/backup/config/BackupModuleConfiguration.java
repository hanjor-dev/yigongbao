package com.yigongbao.module.backup.config;

import com.yigongbao.module.backup.security.config.BackupSecurityConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 线上备份协作模块入口配置。
 *
 * <p>业务领域 Bean 继续由主应用的 {@code com.yigongbao} 扫描范围发现，本配置只集中注册模块属性和安全入口。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BackupProperties.class)
@Import(BackupSecurityConfiguration.class)
public class BackupModuleConfiguration {
}
