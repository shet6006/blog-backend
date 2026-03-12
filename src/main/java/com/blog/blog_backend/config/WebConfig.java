package com.blog.blog_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정 - 프론트엔드(localhost:3000)에서 API 호출 시 필요
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://43.201.28.188",                 // EC2 직접 테스트용 (원하면)
            "https://d31a39ohh8f12p.cloudfront.net", // CloudFront
            "https://kimdongwon.me", // 배포용
            "https://www.kimdongwon.me" // 배포용
    )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
