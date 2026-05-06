package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.api.dto.UpdateUserGradeRequest;
import com.khwajeong.hwpx_saas.db.AdminLoginLogEntity;
import com.khwajeong.hwpx_saas.db.AdminLoginLogRepository;
import com.khwajeong.hwpx_saas.db.AdminSessionEntity;
import com.khwajeong.hwpx_saas.db.AdminSessionRepository;
import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.LoginLogEntity;
import com.khwajeong.hwpx_saas.db.LoginLogRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AdminSessionRepository adminSessionRepository;

    @Value("${app.db.path:/var/data/hwpx.db}")
    private String dbPath;

    public AdminController(
            UserRepository userRepository,
            AdminLoginLogRepository adminLoginLogRepository,
            LoginLogRepository loginLogRepository,
            ApiKeyRepository apiKeyRepository,
            AdminSessionRepository adminSessionRepository
    ) {
        this.userRepository = userRepository;
        this.adminLoginLogRepository = adminLoginLogRepository;
        this.loginLogRepository = loginLogRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.adminSessionRepository = adminSessionRepository;
    }

    @GetMapping("/users")
    public List<UserEntity> users() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(UserEntity::getId).reversed())
                .toList();
    }

    @GetMapping("/login-logs")
    public List<LoginLogEntity> loginLogs() {
        return loginLogRepository.findTop100ByOrderByLoginAtDesc();
    }

    @GetMapping("/admin-login-logs")
    public List<AdminLoginLogEntity> adminLoginLogs() {
        return adminLoginLogRepository.findAll();
    }

    @GetMapping("/api-keys")
    public List<ApiKeyEntity> apiKeys() {
        return apiKeyRepository.findAll();
    }

    @GetMapping("/admin-sessions")
    public List<AdminSessionEntity> adminSessions() {
        return adminSessionRepository.findAll();
    }

    @GetMapping("/backup/db")
    public ResponseEntity<?> backupDb() {
        try {
            Path path = Path.of(dbPath);

            if (!Files.exists(path)) {
                return ResponseEntity.status(404).body(error("db file not found: " + dbPath));
            }

            Resource resource = new FileSystemResource(path);

            String timestamp = OffsetDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            String filename = "hwpx-backup-" + timestamp + ".db";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\""
                    )
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(error("db backup failed"));
        }
    }

    @PatchMapping("/users/{userId}/grade")
    public ResponseEntity<?> updateUserGrade(
            @PathVariable Long userId,
            @RequestBody UpdateUserGradeRequest req
    ) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        if (req == null || req.grade == null || req.grade.isBlank()) {
            return ResponseEntity.badRequest().body(error("grade is required"));
        }

        String nextGrade = req.grade.trim().toUpperCase();
        if (!nextGrade.equals("STARTER")
                && !nextGrade.equals("PRO")
                && !nextGrade.equals("BUSINESS")) {
            return ResponseEntity.badRequest().body(error("invalid grade"));
        }

        user.setGrade(nextGrade);
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("grade", user.getGrade());
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        if ("DELETED".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.badRequest().body(error("deleted user cannot be blocked"));
        }

        user.setStatus("BLOCKED");
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("status", user.getStatus());
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/users/{userId}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        if ("DELETED".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.badRequest().body(error("deleted user cannot be unblocked"));
        }

        user.setStatus("ACTIVE");
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("status", user.getStatus());
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(error("user not found"));
        }

        user.setStatus("DELETED");
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("userId", user.getId());
        res.put("status", user.getStatus());
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", false);
        map.put("message", message);
        return map;
    }
}