package com.qzxx.footprint_planet.config;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.interceptor.JwtTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Resource
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",      // 注册
                        "/api/auth/login",         // 登录
                        "/api/hot-destinations",   // 热门目的地
                        "/api/feedback",           // 提交反馈
                        "/api/upload",             // 上传图片
                        "/api/captcha"             // 图形验证码
                );
    }
}
