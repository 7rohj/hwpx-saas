package com.khwajeong.hwpx_saas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;

public class ApiKeyFilter extends OncePerRequestFilter {

  private final String expectedApiKey;
  private final ObjectMapper om;

  public ApiKeyFilter(String expectedApiKey, ObjectMapper om) {
    this.expectedApiKey = expectedApiKey;
    this.om = om;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // swagger / health 등은 제외
    return path.startsWith("/swagger")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
    try {
      String key = req.getHeader("X-API-Key");

      if (expectedApiKey == null || expectedApiKey.isBlank()) {
        chain.doFilter(req, res); // 개발 단계에서는 키 비활성도 가능
        return;
      }

      if (key == null || !expectedApiKey.equals(key)) {
        ApiErrorResponse body = ApiErrorResponse.of(
            ErrorCode.UNAUTHORIZED,
            "invalid api key",
            null,
            req.getRequestURI()
        );
        byte[] json = om.writeValueAsBytes(body);

        res.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getOutputStream().write(json);
        return;
      }

      chain.doFilter(req, res);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}