package com.qzxx.footprint_planet.common;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class RateLimiter {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setLocation(new ClassPathResource("tryAcquire.lua"));
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
        RATE_LIMIT_SCRIPT.getScriptAsString();
    }

    public boolean tryAcquire(String key, int windowSeconds, int maxCount) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSeconds * 1000L;
            double random = Math.random();
            /*恢复时需补 import: ZSetOperations, TimeUnit
            ZSetOperations<String, Object> stringObjectZSetOperations = redisTemplate.opsForZSet();
            // 1. 移除窗口之前过期的数据
            stringObjectZSetOperations.removeRangeByScore(key, 0, windowStart);
            // 2. 添加本次请求记录，score是当前时间戳，member用now+随机串防止重复
            String member = now + ":" + Math.random();
            stringObjectZSetOperations.add(key, member, now);
            // 3. 查询当前窗口内总数
            Long count = stringObjectZSetOperations.zCard(key);
            // 4. 设置key过期，兜底清理
            redisTemplate.expire(key, windowSeconds + 10, TimeUnit.SECONDS);

            if (count != null && count <= maxCount) {
                return true;
            } else {
                // 超过限制，删掉刚插入的这条记录
                stringObjectZSetOperations.remove(key, member);
                return false;*/
            Long res = redisTemplate.execute(RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    now, windowStart,windowSeconds, maxCount,random);
            return res != null && res == 1;
        } catch (Exception e) {
            // 用 error 而非 warn：这个 catch 既兜「Redis 抖动」也兜「脚本本身有问题」，
            // 后者会让限流静默失效却看起来一切正常，必须能在日志里被搜出来。
            // 只记 message 不打堆栈，避免 Redis 长时间不可用时把日志刷满。
            log.error("限流计数失败，本次放行: key={}, err={}", key, e.getMessage());
            return true;
        }
    }
}

