package com.qzxx.footprint_planet.interceptor;

import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.config.JwtProperties;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtProperties jwtProperties;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //  免登录的公开 GET 接口（路径 + 方法双命中才放行，避免同路径写操作被误放，
        if ("GET".equalsIgnoreCase(request.getMethod()) && isPublicReadPath(request.getRequestURI())) {
            return true;
        }
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }
        //1、获取token
        String token = request.getHeader(jwtProperties.getTokenName());
        //1.1 前端统一以 "Bearer <token>" 携带，剥掉前缀只留 token 本体
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        //2、解析token：缺失/无效/过期统一抛 401 业务异常，
        //   由全局异常处理器返回 {code:401,message,...}（HTTP 200），符合前端解包约定
        log.info("jwt校验:{}", token);
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
        if (claims == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Long userId = Long.valueOf(claims.get("userId").toString());
        String role = claims.get("role").toString();
        BaseContext.setCurrentUser(userId, role);
        //3、通过，放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        BaseContext.remove();
    }

    /** 免登录公开 GET 路径：社区笔记流、他人主页、他人公开规划 */
    private static boolean isPublicReadPath(String uri) {
        return uri.equals("/api/notes")
                || uri.equals("/api/hot-destinations")
                || uri.matches("/api/users/[^/]+")          // 6 他人公开主页
                || uri.matches("/api/users/[^/]+/plans");   // 7 作者公开规划列表
    }
}
