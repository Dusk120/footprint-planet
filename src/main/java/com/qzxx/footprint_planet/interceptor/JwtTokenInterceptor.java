package com.qzxx.footprint_planet.interceptor;

import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.config.JwtProperties;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1、获取token
        String token = request.getHeader(jwtProperties.getTokenName());
        //1.1 前端统一以 "Bearer <token>" 携带，剥掉前缀只留 token 本体
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        //2、免登录的公开 GET 接口（路径 + 方法双命中才放行，避免同路径写操作被误放）。
        //   采用「可选认证」：带了有效 token 且账号仍启用就解析出当前用户（笔记流要据此判断「我点没点过赞」），
        //   没带、已失效或已被禁用一律按匿名处理，照常放行。
        if ("GET".equalsIgnoreCase(request.getMethod()) && isPublicReadPath(request.getRequestURI())) {
            if (StringUtils.hasText(token)) {
                try {
                    Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
                    if (claims != null) {
                        Long userId = Long.valueOf(claims.get("userId").toString());
                        if (isActiveUser(userId)) {
                            BaseContext.setCurrentUserId(userId);
                            BaseContext.setCurrentUserRole(claims.get("role").toString());
                        }
                    }
                } catch (Exception e) {
                    log.debug("公开接口 token 解析失败，按匿名处理: {}", e.getMessage());
                }
            }
            return true;
        }

        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }
        //3、解析token：缺失/无效/过期统一抛 401 业务异常，
        //   由全局异常处理器返回 {code:401,message,...}（HTTP 200），符合前端解包约定
        log.info("jwt校验:{}", token);
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
        if (claims == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Long userId = Long.valueOf(claims.get("userId").toString());
        String role = claims.get("role").toString();

        //4、实时核对账号状态：token 是无状态的快照，只有每次请求回库确认，
        //   被禁用（status=1）或已删除的账号才会「下次请求即被拦截」，否则已签发的 token 在整个有效期内都能用。
        if (!isActiveUser(userId)) {
            throw new BusinessException(401, "账号已被禁用");
        }
        BaseContext.setCurrentUserId(userId);
        BaseContext.setCurrentUserRole(role);

        //5、鉴权：管理端接口仅 ADMIN 可访问（按路径前缀统一判断，新增 /api/admin/** 接口自动生效）
        if (request.getRequestURI().startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "无权限访问");
        }
        //6、通过，放行
        return true;
    }

    /** 账号存在且未被禁用（status = 0）才算有效 */
    private boolean isActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && Integer.valueOf(0).equals(user.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        BaseContext.remove();
    }

    /** 免登录公开 GET 路径：社区笔记流、他人主页、他人公开规划、公开规划详情 */
    private static boolean isPublicReadPath(String uri) {
        return uri.equals("/api/notes")
                || uri.equals("/api/hot-destinations")
                || uri.matches("/api/users/[^/]+")          // 6 他人公开主页
                || uri.matches("/api/users/[^/]+/plans")    // 7 作者公开规划列表
                || uri.matches("/api/plans/\\d+");          // 4.4.2 规划详情（私有由 Service 回 404）
    }
}
