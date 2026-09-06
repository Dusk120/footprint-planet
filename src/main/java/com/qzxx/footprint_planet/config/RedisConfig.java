package com.qzxx.footprint_planet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * key 用 String 序列化（Redis 里可直接读），value 用 GenericJacksonJsonRedisSerializer
     * 后续需要序列化 java.time 或定制 ObjectMapper 时，可用 builder().customize(mapperBuilder -> ...) 扩展。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder().build();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
