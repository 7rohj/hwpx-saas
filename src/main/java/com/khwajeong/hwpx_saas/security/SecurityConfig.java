/*
package com.khwajeong.hwpx_saas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class SecurityConfig {

  @Value("${app.api-key:}")
  private String apiKey;

  @Bean
  public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(ObjectMapper om) {
    FilterRegistrationBean<ApiKeyFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new ApiKeyFilter(apiKey, om));
    reg.addUrlPatterns("/api/*"); // /api 아래만 보호
    reg.setOrder(1);
    return reg;
  }
}
*/