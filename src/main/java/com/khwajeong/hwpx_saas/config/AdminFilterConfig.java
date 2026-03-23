package com.khwajeong.hwpx_saas.config;

import com.khwajeong.hwpx_saas.db.AdminSessionRepository;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.security.AdminAuthFilter;
import com.khwajeong.hwpx_saas.service.AdminTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminFilterConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilterRegistration(
            AdminSessionRepository adminSessionRepository,
            UserRepository userRepository,
            AdminTokenService adminTokenService
    ) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthFilter(adminSessionRepository, userRepository, adminTokenService));
        registration.addUrlPatterns("/api/v1/admin/*");
        registration.setOrder(1);
        return registration;
    }
}