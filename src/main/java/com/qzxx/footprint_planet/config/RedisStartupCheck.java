package com.qzxx.footprint_planet.config;

import com.qzxx.footprint_planet.common.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 把 Redis 设为强依赖：启动时强制 ping，连不上直接终止启动并给出明确报错，
 * 避免「运行到一半才发现验证码等 Redis 功能不可用」的半吊子状态。
 * 启动失败的同时，运行时若 Redis 中途挂掉，各使用方仍保留各自的降级逻辑（回库/容错）。
 */
@Component
@Slf4j
public class RedisStartupCheck implements ApplicationRunner {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis 连接正常: {}", pong);
        } catch (Exception e) {
            throw new BusinessException(500, "Redis 未启动或连接失败，本项目已将 Redis 设为强依赖，请先启动 Redis 再运行。");
        }
    }
}
