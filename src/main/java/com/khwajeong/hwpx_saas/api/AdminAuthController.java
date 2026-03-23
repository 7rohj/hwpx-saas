package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.api.dto.LoginRequest;
import com.khwajeong.hwpx_saas.db.AdminLoginLogEntity;
import com.khwajeong.hwpx_saas.db.AdminLoginLogRepository;
import com.khwajeong.hwpx_saas.db.AdminSessionEntity;
import com.khwajeong.hwpx_saas.db.AdminSessionRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.AdminTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final UserRepository userRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenService adminTokenService;

    public AdminAuthController(
            UserRepository userRepository,
            AdminSessionRepository adminSessionRepository,
            AdminLoginLogRepository adminLoginLogRepository,
            PasswordEncoder passwordEncoder,
            AdminTokenService adminTokenService
    ) {
        this.userRepository = userRepository;
        this.adminSessionRepository = adminSessionRepository;
        this.adminLoginLogRepository = adminLoginLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenService = adminTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req.email == null || req.email.isBlank() || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body(error("email and password are required"));
        }

        String email = req.email.trim();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        UserEntity user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            adminLoginLogRepository.save(createLog(null, email, false, ip, ua, "USER_NOT_FOUND"));
            return ResponseEntity.status(401).body(error("invalid admin email or password"));
        }

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            adminLoginLogRepository.save(createLog(user.getId(), user.getEmail(), false, ip, ua, "WRONG_PASSWORD"));
            return ResponseEntity.status(401).body(error("invalid admin email or password"));
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            adminLoginLogRepository.save(createLog(user.getId(), user.getEmail(), false, ip, ua, "NOT_ADMIN"));
            return ResponseEntity.status(403).body(error("admin access denied"));
        }

        String rawToken = adminTokenService.generateRawToken();
        String tokenHash = adminTokenService.hashToken(rawToken);

        AdminSessionEntity session = new AdminSessionEntity();
        session.setUserId(user.getId());
        session.setTokenHash(tokenHash);
        session.setIssuedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusHours(1));
        session.setIpAddress(ip);
        session.setUserAgent(ua);
        adminSessionRepository.save(session);

        adminLoginLogRepository.save(createLog(user.getId(), user.getEmail(), true, ip, ua, "SUCCESS"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("email", user.getEmail());
        res.put("adminToken", rawToken);
        res.put("expiresAt", session.getExpiresAt());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (adminToken == null || adminToken.isBlank()) {
            return ResponseEntity.badRequest().body(error("admin token is required"));
        }

        String tokenHash = adminTokenService.hashToken(adminToken.trim());
        AdminSessionEntity session = adminSessionRepository.findByTokenHash(tokenHash).orElse(null);

        if (session != null && session.getRevokedAt() == null) {
            session.setRevokedAt(OffsetDateTime.now());
            adminSessionRepository.save(session);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        return ResponseEntity.ok(res);
    }

    private AdminLoginLogEntity createLog(
            Long userId,
            String email,
            boolean success,
            String ip,
            String ua,
            String message
    ) {
        AdminLoginLogEntity log = new AdminLoginLogEntity();
        log.setUserId(userId);
        log.setEmail(email);
        log.setSuccess(success);
        log.setIpAddress(ip);
        log.setUserAgent(ua);
        log.setMessage(message);
        return log;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", false);
        map.put("message", message);
        return map;
    }
}