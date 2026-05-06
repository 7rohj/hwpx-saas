package com.khwajeong.hwpx_saas.security;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.LoginLogEntity;
import com.khwajeong.hwpx_saas.db.LoginLogRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String API_KEY_COOKIE_NAME = "AUTOHWPX_API_KEY";

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogRepository loginLogRepository;

    private static final String FRONTEND_URL = "https://autohwpx.com";

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyGenerator apiKeyGenerator,
            PasswordEncoder passwordEncoder,
            LoginLogRepository loginLogRepository
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.passwordEncoder = passwordEncoder;
        this.loginLogRepository = loginLogRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String provider = extractProvider(authentication);
        String email = extractEmail(oauthUser);

        if (email == null || email.isBlank()) {
            response.sendRedirect(FRONTEND_URL + "/oauth-success?error=email_not_found");
            return;
        }

        email = email.trim();

        UserEntity user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new UserEntity();
            user.setEmail(email);
            user.setProvider(provider);
            user.setPasswordHash(passwordEncoder.encode("OAUTH_LOGIN_" + UUID.randomUUID()));
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

        ResponseCookie cookie = ResponseCookie.from(API_KEY_COOKIE_NAME, apiKey.getApiKey())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        saveLoginLog(user, provider);

        response.sendRedirect(FRONTEND_URL + "/oauth-success");
    }

    private String extractProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId(); // google / kakao / naver
        }

        return "oauth";
    }

    private void saveLoginLog(UserEntity user, String provider) {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            LoginLogEntity log = new LoginLogEntity();
            log.setUserId(user.getId());
            log.setEmail(user.getEmail());
            log.setSuccess(true);
            log.setLoginAt(now);
            log.setMessage("OAUTH_LOGIN_" + provider.toUpperCase());

            loginLogRepository.save(log);

            user.setLastLoginAt(now);
            userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");

        if (email != null && !email.isBlank()) {
            return email;
        }

        Object kakaoAccountObj = oauthUser.getAttribute("kakao_account");

        if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
            Object kakaoEmail = kakaoAccount.get("email");

            if (kakaoEmail instanceof String kakaoEmailStr && !kakaoEmailStr.isBlank()) {
                return kakaoEmailStr;
            }
        }

        Object naverResponseObj = oauthUser.getAttribute("response");

        if (naverResponseObj instanceof Map<?, ?> naverResponse) {
            Object naverEmail = naverResponse.get("email");

            if (naverEmail instanceof String naverEmailStr && !naverEmailStr.isBlank()) {
                return naverEmailStr;
            }
        }

        return null;
    }
}