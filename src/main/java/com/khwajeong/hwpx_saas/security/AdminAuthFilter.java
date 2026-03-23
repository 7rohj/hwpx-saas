package com.khwajeong.hwpx_saas.security;

import com.khwajeong.hwpx_saas.db.AdminSessionEntity;
import com.khwajeong.hwpx_saas.db.AdminSessionRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.AdminTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminSessionRepository adminSessionRepository;
    private final UserRepository userRepository;
    private final AdminTokenService adminTokenService;

    public AdminAuthFilter(
            AdminSessionRepository adminSessionRepository,
            UserRepository userRepository,
            AdminTokenService adminTokenService
    ) {
        this.adminSessionRepository = adminSessionRepository;
        this.userRepository = userRepository;
        this.adminTokenService = adminTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/admin/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // CORS preflight 요청은 인증 없이 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = request.getHeader("X-Admin-Token");

        if (rawToken == null || rawToken.isBlank()) {
            unauthorized(response, "missing admin token");
            return;
        }

        String tokenHash = adminTokenService.hashToken(rawToken.trim());
        AdminSessionEntity session = adminSessionRepository.findByTokenHash(tokenHash).orElse(null);

        if (session == null) {
            unauthorized(response, "invalid admin token");
            return;
        }

        if (session.getRevokedAt() != null) {
            unauthorized(response, "revoked admin token");
            return;
        }

        if (session.getExpiresAt() == null || session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            unauthorized(response, "expired admin token");
            return;
        }

        UserEntity user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            unauthorized(response, "admin access denied");
            return;
        }

        request.setAttribute("adminUserId", user.getId());
        request.setAttribute("adminEmail", user.getEmail());

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"ok\":false,\"message\":\"" + message + "\"}");
    }
}