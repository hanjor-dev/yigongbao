package com.yigongbao.module.order.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class LegacyDataSourceConfig {

    /**
     * 定义主库数据源，避免自定义旧库 DataSource Bean 使 Spring Boot 主库自动配置回退。
     */
    @Bean(name = "primaryDataSourceProperties")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "legacyDataSourceProperties")
    @ConditionalOnProperty(prefix = "yigongbao.legacy-datasource", name = "enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "yigongbao.legacy-datasource")
    public DataSourceProperties legacyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "legacyDataSource")
    @ConditionalOnProperty(prefix = "yigongbao.legacy-datasource", name = "enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "yigongbao.legacy-datasource.hikari")
    public HikariDataSource legacyDataSource(
            @Qualifier("legacyDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "legacyJdbcTemplate")
    @ConditionalOnProperty(prefix = "yigongbao.legacy-datasource", name = "enabled", havingValue = "true")
    public JdbcTemplate legacyJdbcTemplate(@Qualifier("legacyDataSource") DataSource legacyDataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(legacyDataSource);
        jdbcTemplate.setQueryTimeout(60);
        return jdbcTemplate;
    }
}
