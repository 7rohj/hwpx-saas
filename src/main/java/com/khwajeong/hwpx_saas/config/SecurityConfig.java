package com.khwajeong.hwpx_saas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.security.ApiKeyAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

  @Bean
  public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(AppProperties props, ObjectMapper om) {
    FilterRegistrationBean<ApiKeyAuthFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new ApiKeyAuthFilter(props, om));
    bean.addUrlPatterns("/api/*"); // 전체에서 검사 (필터 내부에서 swagger 제외 처리)
    bean.setOrder(1);
    return bean;
  }
}