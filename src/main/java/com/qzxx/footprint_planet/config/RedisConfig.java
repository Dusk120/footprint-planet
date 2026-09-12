package com.qzxx.footprint_planet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
public class RedisConfig {

    /**
     * key 用 String 序列化（Redis 里可直接读），value 用 GenericJacksonJsonRedisSerializer。
     * 必须显式开启 default typing：否则写入的对象不带 @class 类型信息，
     * 读回来一律是 LinkedHashMap，强转回原类型会抛 ClassCastException。
     * 后续需要序列化 java.time 或定制 ObjectMapper 时，可用 builder().customize(mapperBuilder -> ...) 扩展。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        // 只信任本项目与 JDK 集合的类型，避免 default typing 带来的反序列化风险
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.qzxx.footprint_planet")
                .allowIfSubType("java.util")
                .build();
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
