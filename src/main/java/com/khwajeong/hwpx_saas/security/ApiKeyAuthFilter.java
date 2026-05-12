package com.khwajeong.hwpx_saas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-API-Key";

  private final AppProperties props;
  private final ObjectMapper om;

  private static final Set<String> EXCLUDE_PREFIX = Set.of(
      "/swagger-ui",
      "/v3/api-docs",
      "/swagger",
      "/actuator",
      "/health",
      "/oauth2",
      "/login/oauth2"
  );

  public ApiKeyAuthFilter(AppProperties props, ObjectMapper om) {
    this.props = props;
    this.om = om;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();

    for (String p : EXCLUDE_PREFIX) {
      if (path.startsWith(p)) return true;
    }

    if (path.startsWith("/api/v1/auth")) return true;
    if (path.startsWith("/api/v1/admin")) return true;
    if (path.startsWith("/api/v1/feedback/comments")) return true;
    if (path.startsWith("/api/v1/template")) return true;
    if (path.startsWith("/api/v1/generate")) return true;
    if (path.startsWith("/api/v1/dashboard")) return true;
    if (path.startsWith("/api/v1/files")) return true;

    if (path.startsWith("/api/v1/compose/debug")) return true;
    if (path.startsWith("/api/v1/compose/render")) return true;
    if (path.startsWith("/api/v1/compose/heading1/apply")) return true;
    if (path.startsWith("/api/v1/compose/extract")) return true;
    if (path.startsWith("/api/v1/compose/preview")) return true;
    if (path.startsWith("/api/v1/compose/selections")) return true;
    if (path.startsWith("/api/v1/compose/apply")) return true;
    if (path.startsWith("/api/v1/compose/ocr")) return true;
    if (path.startsWith("/api/v1/compose/page-image")) return true;
    if (path.startsWith("/api/v1/compose/html")) return true;
    if (path.startsWith("/api/v1/compose/html-preview")) return true;
    if (path.startsWith("/api/v1/compose/document")) return true;
    
    if (path.startsWith("/api/v1/compose/files")) return true;

    if (path.startsWith("/api/v1/credit-test")) return true;

    // if (path.startsWith("/api/v1/users")) return true;

    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String expected = props.getApiKey();

    String provided = request.getHeader(HEADER_NAME);

    // 구글 로그인 사용자는 HttpOnly Cookie로 API Key를 보냄
    if (provided == null || provided.isBlank()) {
      provided = findCookieValue(request, OAuth2LoginSuccessHandler.API_KEY_COOKIE_NAME);
    }

    if (expected == null || expected.isBlank()) {
      writeError(response, request, ErrorCode.UNAUTHORIZED, "server api-key is not configured", null);
      return;
    }

    if (provided == null || provided.isBlank() || !expected.equals(provided.trim())) {
      writeError(response, request, ErrorCode.UNAUTHORIZED, "invalid api key", null);
      return;
    }

    filterChain.doFilter(request, response);
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

  private void writeError(
      HttpServletResponse response,
      HttpServletRequest request,
      ErrorCode code,
      String message,
      Object detail
  ) throws IOException {

    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiErrorResponse body = ApiErrorResponse.of(code, message, detail, request.getRequestURI());
    om.writeValue(response.getWriter(), body);
  }
}