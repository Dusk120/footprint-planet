package com.qzxx.footprint_planet.common;

/**
 * Redis 键集中管理，避免 key 散落在各 Service / 拦截器里重复定义、日后改 key 漏改。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 用户账号状态缓存（拦截器每次请求核对，禁用/启用时失效） */
    public static final String USER_STATUS_PREFIX = "user:status:";

    /** 热门目的地 Top5 缓存（24h） */
    public static final String HOT_CITIES = "hot_cities";

    /** 图形验证码答案缓存（TTL 2 分钟） */
    public static final String CAPTCHA_PREFIX = "captcha:";

    public static String userStatus(Long userId) {
        return USER_STATUS_PREFIX + userId;
    }

    public static String captcha(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }
}
