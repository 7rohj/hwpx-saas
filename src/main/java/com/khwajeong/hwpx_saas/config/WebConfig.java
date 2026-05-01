package com.khwajeong.hwpx_saas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173", // 로컬 개발
                        "https://autohwpx.khj291400.workers.dev", // 현재 프론트 주소
                        "https://autohwpx.pages.dev", // Pages 주소 (있을 경우)
                        "https://autohwpx.com", // 실제 도메인
                        "https://www.autohwpx.com"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}