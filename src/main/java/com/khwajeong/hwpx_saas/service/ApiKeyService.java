package com.khwajeong.hwpx_saas.service;

import com.khwajeong.hwpx_saas.db.ApiKeyEntity;
import com.khwajeong.hwpx_saas.db.ApiKeyRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.db.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    public UserEntity findUserByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        ApiKeyEntity keyEntity = apiKeyRepository
                .findByApiKeyAndStatus(apiKey.trim(), "ACTIVE")
                .orElse(null);

        if (keyEntity == null) {
            return null;
        }

        return userRepository.findById(keyEntity.getUserId()).orElse(null);
    }
}