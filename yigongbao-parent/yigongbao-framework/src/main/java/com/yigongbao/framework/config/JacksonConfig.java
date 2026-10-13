package com.yigongbao.framework.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
     * 支持反序列化的 LocalDateTime 格式列表
     * 按优先级排序，优先尝试 ISO-8601，再尝试常用格式
     */
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    );

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);

        // 替换为支持多格式的 LocalDateTime 反序列化器
        SimpleModule customTimeModule = new SimpleModule();
        customTimeModule.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());
        objectMapper.registerModule(customTimeModule);

        // 配置序列化：空值不参与序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 配置序列化：禁用日期时间戳（使用字符串格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置反序列化：忽略未知属性（防止新增字段导致反序列化失败）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper;
    }

    /**
     * 支持多种日期时间格式的 LocalDateTime 反序列化器
     * 按顺序尝试解析，兼容 ISO-8601 及常用格式
     */
    private static class MultiFormatLocalDateTimeDeserializer extends LocalDateTimeDeserializer {

        public MultiFormatLocalDateTimeDeserializer() {
            super(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }
            // 依次尝试各格式
            for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter);
                } catch (Exception ignored) {
                    // 尝试下一个格式
                }
            }
            // 所有格式都失败，抛出原始异常信息
            throw new IOException("无法解析日期时间字符串: " + value
                    + "，支持的格式: ISO-8601, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, yyyy/MM/dd HH:mm:ss");
        }
    }
}
