package com.khwajeong.hwpx_saas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.security.ApiKeyAuthFilter;
import com.khwajeong.hwpx_saas.security.OAuth2LoginSuccessHandler;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
    this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/",
                "/health",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/api/v1/auth/**",
                "/oauth2/**",
                "/login/oauth2/**"
            ).permitAll()
            .anyRequest().permitAll()
        )
        .oauth2Login(oauth -> oauth
            .successHandler(oAuth2LoginSuccessHandler)
        );

    return http.build();
  }

  @Bean
  public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(
      AppProperties props,
      ObjectMapper om
  ) {
    FilterRegistrationBean<ApiKeyAuthFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new ApiKeyAuthFilter(props, om));
    bean.addUrlPatterns("/api/*");
    bean.setOrder(1);
    return bean;
  }
}