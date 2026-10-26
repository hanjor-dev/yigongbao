package com.yigongbao.module.backup.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.filter.MachineHmacAuthenticationFilter;
import com.yigongbao.module.backup.security.mapper.MachineIdentityMapper;
import com.yigongbao.module.backup.security.mapper.MachineKeyMapper;
import com.yigongbao.module.backup.security.mapper.MachineNonceMapper;
import com.yigongbao.module.backup.security.service.ConfiguredMachineSecretProvider;
import com.yigongbao.module.backup.security.service.DatabaseMachineCredentialResolver;
import com.yigongbao.module.backup.security.service.MachineCredentialResolver;
import com.yigongbao.module.backup.security.service.MachineSecretProvider;
import com.yigongbao.module.backup.security.service.NonceRegistry;
import com.yigongbao.module.backup.security.service.PersistentNonceRegistry;
import com.yigongbao.module.backup.security.verifier.HmacMachineRequestVerifier;
import com.yigongbao.module.backup.security.verifier.MachineRequestVerifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;

/** 备份内部机器接口安全入口配置。 */
@Configuration(proxyBeanMethods = false)
public class BackupSecurityConfiguration {

    @Bean
    public MachineCredentialResolver machineCredentialResolver(
            ObjectProvider<MachineKeyMapper> keyMappers,
            ObjectProvider<MachineIdentityMapper> identityMappers
    ) {
        return new DatabaseMachineCredentialResolver(keyMappers, identityMappers);
    }

    @Bean
    public MachineSecretProvider machineSecretProvider(Environment environment) {
        return new ConfiguredMachineSecretProvider(environment);
    }

    @Bean
    public NonceRegistry nonceRegistry(ObjectProvider<MachineNonceMapper> nonceMappers) {
        return new PersistentNonceRegistry(nonceMappers);
    }

    @Bean
    public MachineRequestVerifier machineRequestVerifier(
            MachineCredentialResolver credentialResolver,
            MachineSecretProvider secretProvider,
            NonceRegistry nonceRegistry,
            BackupProperties properties
    ) {
        return new HmacMachineRequestVerifier(
                credentialResolver, secretProvider, nonceRegistry, properties);
    }

    /** 注册内部接口 HMAC 过滤器入口。 */
    @Bean
    public MachineHmacAuthenticationFilter machineHmacAuthenticationFilter(
            BackupProperties properties,
            Environment environment,
            ObjectMapper objectMapper,
            MachineRequestVerifier verifier
    ) {
        return new MachineHmacAuthenticationFilter(properties, environment, objectMapper, verifier);
    }

    /**
     * 将过滤器限制到内部接口前缀，并安排在请求追踪过滤器之后执行。
     */
    @Bean
    public FilterRegistrationBean<MachineHmacAuthenticationFilter> machineHmacFilterRegistration(
            MachineHmacAuthenticationFilter filter
    ) {
        FilterRegistrationBean<MachineHmacAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("machineHmacAuthenticationFilter");
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
