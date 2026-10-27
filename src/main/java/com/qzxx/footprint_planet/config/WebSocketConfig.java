package com.qzxx.footprint_planet.config;

import com.qzxx.footprint_planet.interceptor.JwtChannelInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 配置。
 * 端点 /ws（原生 WebSocket，不带 SockJS），服务端推送走 SimpleBroker。
 * 按用户精准推送用 SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload)，
 * 前端对应订阅 /user/queue/notifications。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Resource
    private JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 服务端 -> 客户端 的 broker 前缀；/user 供 convertAndSendToUser 按用户寻址
        registry.enableSimpleBroker("/queue", "/topic");
        // 客户端 -> 服务端 的应用前缀（本项目暂不需要 @MessageMapping，预留）
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // CONNECT 帧鉴权：解析 JWT，把 userId 注入 Principal
        registration.interceptors(jwtChannelInterceptor);
    }
}
