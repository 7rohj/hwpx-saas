package com.khwajeong.hwpx_saas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
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

    return path.startsWith("/swagger")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/actuator")
        || path.startsWith("/health")
        || path.startsWith("/oauth2")
        || path.startsWith("/login/oauth2")
        || path.startsWith("/api/v1/auth");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req,
      HttpServletResponse res,
      FilterChain chain
  ) {
    try {
      String key = req.getHeader("X-API-Key");

      // 구글 로그인 사용자는 HttpOnly Cookie에 API Key가 들어있음
      if (key == null || key.isBlank()) {
        key = findCookieValue(req, OAuth2LoginSuccessHandler.API_KEY_COOKIE_NAME);
      }

      if (expectedApiKey == null || expectedApiKey.isBlank()) {
        chain.doFilter(req, res);
        return;
      }

      if (key == null || key.isBlank() || !expectedApiKey.equals(key)) {
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

  private String findCookieValue(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}