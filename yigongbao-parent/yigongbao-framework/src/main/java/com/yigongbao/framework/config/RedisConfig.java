package com.yigongbao.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 配置 RedisTemplate 的序列化方式，支持 Java 对象存储
 *
 * @author hanjor
 * @date 2026-03-14 15:00:00
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * 使用 Jackson2JsonRedisSerializer 序列化 value，支持 LocalDateTime 等类型
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册 JavaTimeModule 支持 LocalDateTime
        mapper.registerModule(new JavaTimeModule());
        // 禁用日期时间戳，使用字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用 Jackson2JsonRedisSerializer 序列化 value
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // key 使用 StringRedisSerializer
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 使用 Jackson2JsonRedisSerializer
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
