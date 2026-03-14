package com.yigongbao.framework.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;

/**
 * Jackson 配置类
 * 统一配置 JSON 序列化与反序列化的行为
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 ObjectMapper
     * 设置日期格式、null 值处理、类型识别等
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 Java 8 时间模块，支持 LocalDateTime、LocalDate 等类型
        objectMapper.registerModule(new JavaTimeModule());

        // 配置日期格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 配置序列化：空值不参与序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 配置序列化：禁用日期时间戳（使用字符串格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置反序列化：忽略未知属性（防止新增字段导致反序列化失败）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper;
    }

}
