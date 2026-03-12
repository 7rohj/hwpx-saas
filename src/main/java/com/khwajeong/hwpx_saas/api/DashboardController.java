package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// @Tag(name = "99. Dashboard Controller", description = "테스트중")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public DashboardController(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyGenerator apiKeyGenerator
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @GetMapping("/api-key")
    public ResponseEntity<?> getApiKey(@RequestParam String email) {
        UserEntity user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        ApiKeyEntity apiKey = apiKeyRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                .orElse(null);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("email", user.getEmail());
        res.put("apiKey", apiKey != null ? apiKey.getApiKey() : null);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/api-key/regenerate")
    public ResponseEntity<?> regenerateApiKey(@RequestParam String email) {
        UserEntity user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        apiKeyRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                .ifPresent(oldKey -> {
                    oldKey.setStatus("REVOKED");
                    oldKey.setRevokedAt(OffsetDateTime.now());
                    apiKeyRepository.save(oldKey);
                });

        ApiKeyEntity newKey = new ApiKeyEntity();
        newKey.setUserId(user.getId());
        newKey.setApiKey(apiKeyGenerator.generate());
        newKey.setStatus("ACTIVE");
        apiKeyRepository.save(newKey);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("email", user.getEmail());
        res.put("apiKey", newKey.getApiKey());

        return ResponseEntity.ok(res);
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", false);
        map.put("message", message);
        return map;
    }
}