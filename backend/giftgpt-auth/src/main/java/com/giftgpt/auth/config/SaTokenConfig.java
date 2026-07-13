package com.giftgpt.auth.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new cn.dev33.satoken.interceptor.SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/products/**",
                    "/api/v1/stories",
                    "/api/v1/stories/**",
                    "/api/v1/calendar/**",
                    "/api/v1/greetings/**",
                    "/api/v1/kg/status",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                );
    }
}
