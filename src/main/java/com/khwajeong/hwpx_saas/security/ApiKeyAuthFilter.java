package com.khwajeong.hwpx_saas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

  // 인증 제외할 경로들 (swagger, health 등)
  private static final Set<String> EXCLUDE_PREFIX = Set.of(
      "/swagger-ui", "/v3/api-docs", "/swagger", "/actuator"
  );

  public ApiKeyAuthFilter(AppProperties props, ObjectMapper om) {
    this.props = props;
    this.om = om;
  }

  // 인증방식 2단계 -> 개인 키 1개로 수정
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
      String path = request.getRequestURI();

      for (String p : EXCLUDE_PREFIX) {
          if (path.startsWith(p)) return true;
      }

      // 사용자 API는 공용키 필터에서 제외
      if (path.startsWith("/api/v1/auth")) return true;
      if (path.startsWith("/api/v1/admin")) return true;
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
      if (path.startsWith("/api/v1/compose/selections/{sessionId}")) return true;
      if (path.startsWith("/api/v1/compose/apply")) return true;

      if (path.startsWith("/api/v1/compose/ocr")) return true;

      return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    // 우리 API만 보호하고 싶으면 여기서 제한 가능
    // 예: if (!request.getRequestURI().startsWith("/api/v1")) { filterChain.doFilter(...); return; }
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

    String expected = props.getApiKey();
    String provided = request.getHeader(HEADER_NAME);

    if (expected == null || expected.isBlank()) {
      // 서버 설정 자체가 없으면 개발 중이라도 막는 게 안전
      writeError(response, request, ErrorCode.UNAUTHORIZED, "server api-key is not configured", null);
      return;
    }

    if (provided == null || provided.isBlank() || !expected.equals(provided.trim())) {
      writeError(response, request, ErrorCode.UNAUTHORIZED, "invalid api key", null);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeError(HttpServletResponse response, HttpServletRequest request, ErrorCode code, String message, Object detail)
      throws IOException {

    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiErrorResponse body = ApiErrorResponse.of(code, message, detail, request.getRequestURI());
    om.writeValue(response.getWriter(), body);
  }
}