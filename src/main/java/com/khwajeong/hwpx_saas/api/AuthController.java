package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.api.dto.LoginRequest;
import com.khwajeong.hwpx_saas.api.dto.SignupRequest;
import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.LoginLogEntity;
import com.khwajeong.hwpx_saas.db.LoginLogRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import com.khwajeong.hwpx_saas.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "naver.com",
            "gmail.com",
            "kakao.com",
            "daum.net",
            "hanmail.net"
    );

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyGenerator apiKeyGenerator;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            LoginLogRepository loginLogRepository,
            PasswordEncoder passwordEncoder,
            ApiKeyGenerator apiKeyGenerator,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyGenerator = apiKeyGenerator;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/email-code/send")
    public ResponseEntity<?> sendEmailCode(
            @RequestBody EmailCodeRequest req,
            HttpServletRequest request
    ) {
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.badRequest().body(error("email is required"));
        }

        String email = req.email.trim().toLowerCase();

        String domainError = validateEmailDomain(email);
        if (domainError != null) {
            return ResponseEntity.badRequest().body(error(domainError));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(error("email already exists"));
        }

        String clientIp = getClientIp(request);

        try {
            emailVerificationService.sendCode(email, clientIp);

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("message", "인증번호를 발송했습니다.");
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(error("인증번호 발송 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/email-code/verify")
    public ResponseEntity<?> verifyEmailCode(@RequestBody EmailCodeRequest req) {
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.badRequest().body(error("email is required"));
        }

        if (req.code == null || req.code.isBlank()) {
            return ResponseEntity.badRequest().body(error("인증번호를 입력해주세요."));
        }

        String email = req.email.trim().toLowerCase();

        try {
            emailVerificationService.verifyCode(email, req.code);

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("message", "이메일 인증이 완료되었습니다.");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.badRequest().body(error("email is required"));
        }

        String email = req.email.trim().toLowerCase();

        String domainError = validateEmailDomain(email);
        if (domainError != null) {
            return ResponseEntity.badRequest().body(error(domainError));
        }

        if (!emailVerificationService.isVerified(email)) {
            return ResponseEntity.badRequest().body(error("이메일 인증을 먼저 완료해주세요."));
        }

        if (req.password == null || req.password.length() < 8) {
            return ResponseEntity.badRequest().body(error("password must be at least 8 characters"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(error("email already exists"));
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setRole("USER");
        user.setGrade("STARTER");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setUserId(user.getId());
        apiKey.setApiKey(apiKeyGenerator.generate());
        apiKey.setStatus("ACTIVE");
        apiKeyRepository.save(apiKey);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("email", user.getEmail());
        res.put("apiKey", apiKey.getApiKey());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.email == null || req.email.isBlank() || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body(error("email and password are required"));
        }

        String email = req.email.trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            loginLogRepository.save(createLog(null, email, false, "USER_NOT_FOUND"));
            return ResponseEntity.status(401).body(error("invalid email or password"));
        }

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            loginLogRepository.save(createLog(user.getId(), user.getEmail(), false, "WRONG_PASSWORD"));
            return ResponseEntity.status(401).body(error("invalid email or password"));
        }

        if ("BLOCKED".equalsIgnoreCase(user.getStatus())) {
            loginLogRepository.save(createLog(user.getId(), user.getEmail(), false, "BLOCKED_USER"));
            return ResponseEntity.status(403).body(error("blocked user"));
        }

        if ("DELETED".equalsIgnoreCase(user.getStatus())) {
            loginLogRepository.save(createLog(user.getId(), user.getEmail(), false, "DELETED_USER"));
            return ResponseEntity.status(403).body(error("deleted user"));
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        loginLogRepository.save(createLog(user.getId(), user.getEmail(), true, "SUCCESS"));

        ApiKeyEntity apiKey = apiKeyRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                .orElse(null);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("email", user.getEmail());
        res.put("apiKey", apiKey != null ? apiKey.getApiKey() : null);

        return ResponseEntity.ok(res);
    }

    private String validateEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "올바른 이메일 형식이 아닙니다.";
        }

        int atIndex = email.lastIndexOf("@");
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return "올바른 이메일 형식이 아닙니다.";
        }

        String domain = email.substring(atIndex + 1).toLowerCase();

        if (!ALLOWED_EMAIL_DOMAINS.contains(domain)) {
            return "네이버, 구글, 카카오, 다음 이메일만 가입할 수 있습니다.";
        }

        return null;
    }

    private LoginLogEntity createLog(Long userId, String email, boolean success, String message) {
        LoginLogEntity log = new LoginLogEntity();
        log.setUserId(userId);
        log.setEmail(email);
        log.setSuccess(success);
        log.setMessage(message);
        return log;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", false);
        map.put("message", message);
        return map;
    }

    public static class EmailCodeRequest {
        public String email;
        public String code;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}