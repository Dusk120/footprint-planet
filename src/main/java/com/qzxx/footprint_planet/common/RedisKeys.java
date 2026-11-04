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

    /** 高德算路结果缓存（TTL 7 天） */
    public static final String ROUTE_PREFIX = "route:";

    /**
     * 算路接口限流计数前缀（rate:route:{userId}）。
     * 刻意与缓存前缀 route: 分开命名：redis-cli 排查时能一眼区分「缓存」和「计数器」，
     * 也避免将来做 route:* 缓存清理时把计数器一起删掉。
     */
    public static final String ROUTE_RATE_PREFIX = "rate:route:";

    public static String userStatus(Long userId) {
        return USER_STATUS_PREFIX + userId;
    }

    public static String captcha(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }

    public static String route(String fromCoord, String toCoord, String mode) {
        return ROUTE_PREFIX + fromCoord + ":" + toCoord + ":" + mode;
    }

    /**
     * 算路接口的限流计数 key（滑动窗口实现下，一个用户一个 ZSET，不需要带时间窗口）。
     * ZSET 的 score 存请求时间戳，member 存本次请求标识，窗口靠 removeRangeByScore 滚动。
     *
     * @param userId 用户 ID（该接口需登录，取 BaseContext.getCurrentId()）
     * @return 形如 rate:route:12
     */
    public static String routeRateLimit(Long userId) {
        return ROUTE_RATE_PREFIX + userId;
    }
}
