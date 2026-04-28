package com.khwajeong.hwpx_saas.security;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final PasswordEncoder passwordEncoder;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyGenerator apiKeyGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendRedirect("http://localhost:5173/oauth-success?error=email_not_found");
            return;
        }

        email = email.trim();

        UserEntity user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new UserEntity();
            user.setEmail(email);

            // 구글 로그인 유저도 기존 UserEntity 구조를 맞추기 위해 랜덤 비밀번호 해시 저장
            user.setPasswordHash(passwordEncoder.encode("GOOGLE_LOGIN_" + UUID.randomUUID()));

            user = userRepository.save(user);
        }

        ApiKeyEntity apiKey = apiKeyRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                .orElse(null);

        if (apiKey == null) {
            apiKey = new ApiKeyEntity();
            apiKey.setUserId(user.getId());
            apiKey.setApiKey(apiKeyGenerator.generate());
            apiKey.setStatus("ACTIVE");
            apiKey = apiKeyRepository.save(apiKey);
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString("http://localhost:5173/oauth-success")
                .queryParam("email", user.getEmail())
                .queryParam("apiKey", apiKey.getApiKey())
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}