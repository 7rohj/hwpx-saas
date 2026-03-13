package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import com.khwajeong.hwpx_saas.db.GeneratedFileEntity;
import com.khwajeong.hwpx_saas.db.GeneratedFileRepository;
import com.khwajeong.hwpx_saas.db.UsageLogEntity;
import com.khwajeong.hwpx_saas.db.UsageLogRepository;
import com.khwajeong.hwpx_saas.service.ApiKeyService;
import com.khwajeong.hwpx_saas.service.ApiKeyGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

// @Tag(name = "99. Dashboard Controller", description = "테스트중")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final GeneratedFileRepository generatedFileRepository;
    private final UsageLogRepository usageLogRepository;
    private final ApiKeyService apiKeyService;

    public DashboardController(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyGenerator apiKeyGenerator,
            GeneratedFileRepository generatedFileRepository,
            UsageLogRepository usageLogRepository,
            ApiKeyService apiKeyService
    ) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.generatedFileRepository = generatedFileRepository;
        this.usageLogRepository = usageLogRepository;
        this.apiKeyService = apiKeyService;
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

    @GetMapping("/generated-files")
    public ResponseEntity<?> getGeneratedFiles(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        UserEntity user = apiKeyService.findUserByApiKey(apiKey);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid or missing api key"));
        }

        List<GeneratedFileEntity> files = generatedFileRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> items = new ArrayList<>();
        for (GeneratedFileEntity file : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", file.getId());
            item.put("templateId", file.getTemplateId());
            item.put("fileId", file.getFileId());
            item.put("originalFilename", file.getOriginalFilename());
            item.put("r2ObjectKey", file.getR2ObjectKey());
            item.put("size", file.getSize());
            item.put("status", file.getStatus());
            item.put("createdAt", file.getCreatedAt());
            item.put("downloadUrl", "/api/v1/files/" + file.getFileId());
            items.add(item);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("count", items.size());
        res.put("items", items);

        return ResponseEntity.ok(res);
    }

    @GetMapping("/usage-logs")
    public ResponseEntity<?> getUsageLogs(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        UserEntity user = apiKeyService.findUserByApiKey(apiKey);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid or missing api key"));
        }

        List<UsageLogEntity> logs = usageLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> items = new ArrayList<>();
        for (UsageLogEntity log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("actionType", log.getActionType());
            item.put("targetId", log.getTargetId());
            item.put("success", log.getSuccess());
            item.put("message", log.getMessage());
            item.put("createdAt", log.getCreatedAt());
            items.add(item);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("count", items.size());
        res.put("items", items);

        return ResponseEntity.ok(res);
    }
}