package com.qzxx.footprint_planet.interceptor;

import com.qzxx.footprint_planet.common.PlanRole;
import com.qzxx.footprint_planet.config.JwtProperties;
import com.qzxx.footprint_planet.service.PlanService;
import com.qzxx.footprint_planet.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
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

    @Resource
    private PlanService planService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getCommand() != null) {
            StompCommand command = accessor.getCommand();

            // CONNECT：解析 JWT，把 userId 注入 Principal（供 convertAndSendToUser 寻址）
            if (StompCommand.CONNECT.equals(command)) {
                String auth = accessor.getFirstNativeHeader("Authorization");
                if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
                    if (claims != null) {
                        String userId = String.valueOf(claims.get("userId"));
                        accessor.setUser(() -> userId);
                    }
                }
            }

            // SUBSCRIBE：订阅 /topic/plan/{id} 时校验是否为该计划成员（非成员拒绝）
            if (StompCommand.SUBSCRIBE.equals(command)) {
                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/topic/plan/")) {
                    Long planId;
                    try {
                        planId = Long.parseLong(destination.substring("/topic/plan/".length()));
                    } catch (NumberFormatException e) {
                        throw new MessagingException("订阅地址不合法");
                    }
                    if (accessor.getUser() == null) {
                        throw new MessagingException("未登录，无法订阅该计划");
                    }
                    PlanRole role = planService.getRole(planId, Long.valueOf(accessor.getUser().getName()));
                    if (role == null) {
                        throw new MessagingException("无权限订阅该计划");
                    }
                }
            }
        }
        return message;
    }
}
