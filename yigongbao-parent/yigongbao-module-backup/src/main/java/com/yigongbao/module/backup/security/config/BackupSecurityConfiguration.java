package com.yigongbao.module.backup.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.filter.MachineHmacAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/** 备份内部机器接口安全入口配置。 */
@Configuration(proxyBeanMethods = false)
public class BackupSecurityConfiguration {

    /** 注册 Task 3 将继续完善的 HMAC 过滤器入口。 */
    @Bean
    public MachineHmacAuthenticationFilter machineHmacAuthenticationFilter(
            BackupProperties properties,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        return new MachineHmacAuthenticationFilter(properties, environment, objectMapper);
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
