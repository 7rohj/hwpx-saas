package com.khwajeong.hwpx_saas.api;

import com.khwajeong.hwpx_saas.db.AdminLoginLogEntity;
import com.khwajeong.hwpx_saas.db.AdminLoginLogRepository;
import com.khwajeong.hwpx_saas.db.AdminSessionEntity;
import com.khwajeong.hwpx_saas.db.AdminSessionRepository;
import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AdminSessionRepository adminSessionRepository;

    public AdminController(
            UserRepository userRepository,
            AdminLoginLogRepository adminLoginLogRepository,
            ApiKeyRepository apiKeyRepository,
            AdminSessionRepository adminSessionRepository
    ) {
        this.userRepository = userRepository;
        this.adminLoginLogRepository = adminLoginLogRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.adminSessionRepository = adminSessionRepository;
    }

    @GetMapping("/users")
    public List<UserEntity> users() {
        return userRepository.findAll();
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
}