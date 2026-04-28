package com.khwajeong.hwpx_saas.auth.controller;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class OAuthSessionController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public OAuthSessionController(
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        String apiKey = findCookieValue(request, OAuth2LoginSuccessHandler.API_KEY_COOKIE_NAME);

        if (apiKey == null || apiKey.isBlank()) {
            return Map.of(
                    "ok", false,
                    "message", "not logged in"
            );
        }

        ApiKeyEntity keyEntity = apiKeyRepository.findByApiKey(apiKey).orElse(null);

        if (keyEntity == null || !"ACTIVE".equals(keyEntity.getStatus())) {
            return Map.of(
                    "ok", false,
                    "message", "invalid api key"
            );
        }

        UserEntity user = userRepository.findById(keyEntity.getUserId()).orElse(null);

        if (user == null) {
            return Map.of(
                    "ok", false,
                    "message", "user not found"
            );
        }

        return Map.of(
                "ok", true,
                "email", user.getEmail()
        );
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