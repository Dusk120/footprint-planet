package com.qzxx.footprint_planet.interceptor;

import com.qzxx.footprint_planet.config.JwtProperties;
import com.qzxx.footprint_planet.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket STOMP 连接鉴权：解析 CONNECT 帧里的 Authorization: Bearer <token>，
 * 校验通过把 userId 注入 Principal.name —— convertAndSendToUser 靠它把消息路由到对应连接。
 * 解析失败不抛异常（保持匿名连接），由业务侧决定是否放行。
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
                if (claims != null) {
                    String userId = String.valueOf(claims.get("userId"));
                    // Principal 是函数式接口，name = userId，供 convertAndSendToUser 寻址
                    accessor.setUser(() -> userId);
                }
            }
        }
        return message;
    }
}
