package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.common.RedisKeys;
import com.qzxx.footprint_planet.service.RouteProxyService;
import com.qzxx.footprint_planet.vo.RouteResult;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 高德 Web 服务算路代理：真实调用 restapi.amap.com 的路径规划接口，带 Redis 缓存降级。
 * driving/walking/transit 均走 v3（返回 route.*）。骑行接口需企业认证，已移除。
 */
@Service
public class RouteProxyServiceImpl implements RouteProxyService {

    @Value("${footprint.amap.web-key:}")
    private String amapWebKey;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 必须显式设置连接/读取超时：RestClient.create() 默认不限时，
     * 高德响应慢或网络异常时会把 Tomcat 线程一直占住，几个卡住的请求就能拖慢整站。
     * 正常算路 200ms~3s，超过 3s 基本是异常，快速失败后由调用方降级为「该方式无结果」。
     */
    private final RestClient restClient = RestClient.builder()
            .requestFactory(createRequestFactory())
            .build();

    private static ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    @Override
    public RouteResult calculateLeg(String fromCoord, String toCoord, String mode) {
        String m = (mode == null || mode.isBlank()) ? "driving" : mode;
        String cacheKey = RedisKeys.route(fromCoord, toCoord, m);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof RouteResult r) {
            return r;
        }
        RouteResult r = doCalculate(fromCoord, toCoord, m);
        if (r != null) {
            redisTemplate.opsForValue().set(cacheKey, r, 7, TimeUnit.DAYS);
        }
        return r;
    }

    private RouteResult doCalculate(String fromCoord, String toCoord, String mode) {
        try {
            // 公交需要城市参数，先逆地理编码取起点所在城市
            String city = "transit".equals(mode) ? reverseGeocodeCity(fromCoord) : "";
            String url = buildUrl(mode, fromCoord, toCoord, city);
            JsonNode root = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (root == null || !"1".equals(root.path("status").asText())) {
                return null;
            }
            return parse(mode, root);
        } catch (Exception e) {
            // 算路失败（含无权限/超时/网络异常）静默返回 null，由调用方跳过该交通方式
            return null;
        }
    }

    private RouteResult parse(String mode, JsonNode root) {
        JsonNode container = root.path("route");
        int distance;
        int duration;
        StringBuilder polyline = new StringBuilder();

        if ("transit".equals(mode)) {
            JsonNode transits = container.path("transits");
            if (!transits.isArray() || transits.size() == 0) return null;
            JsonNode transit = transits.get(0);
            distance = transit.path("distance").asInt();
            duration = transit.path("duration").asInt();
            JsonNode segments = transit.path("segments");
            for (int i = 0; i < segments.size(); i++) {
                JsonNode seg = segments.get(i);
                // 步行段
                appendSteps(seg.path("walking").path("steps"), polyline);
                // 公交段
                JsonNode buslines = seg.path("bus").path("buslines");
                for (int j = 0; j < buslines.size(); j++) {
                    append(polyline, buslines.get(j).path("polyline").asText());
                }
            }
        } else {
            JsonNode paths = container.path("paths");
            if (!paths.isArray() || paths.size() == 0) return null;
            JsonNode path0 = paths.get(0);
            distance = path0.path("distance").asInt();
            duration = path0.path("duration").asInt();
            appendSteps(path0.path("steps"), polyline);
        }

        if (distance <= 0 || duration <= 0 || polyline.length() == 0) {
            return null;
        }
        RouteResult r = new RouteResult();
        r.setDistance(distance);
        r.setDuration(duration);
        r.setPolyline(polyline.toString());
        r.setSummary(summary(mode, duration, distance));
        return r;
    }

    private void appendSteps(JsonNode steps, StringBuilder sb) {
        if (steps == null || !steps.isArray()) return;
        for (int i = 0; i < steps.size(); i++) {
            append(sb, steps.get(i).path("polyline").asText());
        }
    }

    private void append(StringBuilder sb, String polyline) {
        if (polyline == null || polyline.isBlank()) return;
        if (sb.length() > 0) sb.append(";");
        sb.append(polyline);
    }

    private String summary(String mode, int durationSeconds, int distanceMeters) {
        int minutes = Math.max(1, durationSeconds / 60);
        double km = Math.round(distanceMeters / 100.0) / 10.0;
        String label = switch (mode) {
            case "walking" -> "步行";
            case "transit" -> "公交";
            default -> "驾车";
        };
        return label + "约 " + minutes + " 分钟 · " + km + " 公里";
    }

    private String reverseGeocodeCity(String coord) {
        try {
            String url = "https://restapi.amap.com/v3/geocode/regeo?location=" + coord + "&key=" + amapWebKey;
            JsonNode root = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (root == null || !"1".equals(root.path("status").asText())) return "";
            JsonNode city = root.path("regeocode").path("addressComponent").path("city");
            if (city.isArray()) {
                return city.size() > 0 ? city.get(0).asText() : "";
            }
            return city.asText();
        } catch (Exception e) {
            return "";
        }
    }

    private String buildUrl(String mode, String from, String to, String city) {
        String path = switch (mode) {
            case "walking" -> "/v3/direction/walking";
            case "transit" -> "/v3/direction/transit/integrated";
            default -> "/v3/direction/driving";
        };
        StringBuilder sb = new StringBuilder("https://restapi.amap.com")
                .append(path)
                .append("?origin=").append(from)
                .append("&destination=").append(to)
                .append("&key=").append(amapWebKey);
        if ("transit".equals(mode)) {
            String c = city == null ? "" : URLEncoder.encode(city, StandardCharsets.UTF_8);
            sb.append("&city=").append(c);
        }
        return sb.toString();
    }
}
