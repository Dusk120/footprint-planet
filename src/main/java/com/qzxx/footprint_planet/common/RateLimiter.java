package com.qzxx.footprint_planet.common;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimiter {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public boolean tryAcquire(String key, int windowSeconds, int maxCount) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSeconds * 1000L;

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
                return false;
            }
        } catch (Exception e) {
            // 只记 message，不打堆栈：Redis 长时间不可用时会每分钟刷满日志
            log.warn("限流计数失败，本次放行: key={}, err={}", key, e.getMessage());
            return true;
        }
    }
}

