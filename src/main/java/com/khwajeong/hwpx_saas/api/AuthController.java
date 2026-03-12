package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.api.dto.LoginRequest;
import com.khwajeong.hwpx_saas.api.dto.SignupRequest;
import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Auth Controller", description = "테스트중")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyGenerator apiKeyGenerator;

    public AuthController(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            PasswordEncoder passwordEncoder,
            ApiKeyGenerator apiKeyGenerator
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.badRequest().body(error("email is required"));
        }
        if (req.password == null || req.password.length() < 8) {
            return ResponseEntity.badRequest().body(error("password must be at least 8 characters"));
        }
        if (userRepository.existsByEmail(req.email.trim())) {
            return ResponseEntity.badRequest().body(error("email already exists"));
        }

        UserEntity user = new UserEntity();
        user.setEmail(req.email.trim());
        user.setPasswordHash(passwordEncoder.encode(req.password));
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

        UserEntity user = userRepository.findByEmail(req.email.trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(error("invalid email or password"));
        }

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(error("invalid email or password"));
        }

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

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", false);
        map.put("message", message);
        return map;
    }
}