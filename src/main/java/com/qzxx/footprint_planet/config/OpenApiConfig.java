package com.qzxx.footprint_planet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI footprintOpenApi() {
        return new OpenAPI().info(new Info()
                .title("足迹星球项目接口文档")
                .description("足迹星球（旅行足迹 / 旅行社区）Web 应用后端接口文档")
                .version("1.0"));
    }
}
