package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.service.R2StorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ComposeOutputStorageService {

    private final R2StorageService r2StorageService;

    public ComposeOutputStorageService(R2StorageService r2StorageService) {
        this.r2StorageService = r2StorageService;
    }

    public StoredComposeFile uploadOutput(Path localOutputPath, String userEmail, String sessionId) {
        try {
            if (localOutputPath == null || !Files.exists(localOutputPath)) {
                throw new IllegalArgumentException("compose output file does not exist: " + localOutputPath);
            }

            byte[] bytes = Files.readAllBytes(localOutputPath);

            String safeEmail = sanitize(userEmail == null || userEmail.isBlank() ? "anonymous" : userEmail);
            String safeSessionId = sanitize(sessionId == null || sessionId.isBlank() ? "unknown-session" : sessionId);

            String fileId = safeSessionId + ".hwpx";
            String objectKey = "compose/" + safeEmail + "/" + fileId;

            r2StorageService.uploadHwpx(bytes, objectKey);

            return new StoredComposeFile(
                    fileId,
                    objectKey,
                    bytes.length,
                    "/api/v1/compose/files/" + fileId + "?userEmail=" + safeEmail
            );

        } catch (IOException e) {
            throw new RuntimeException("compose output upload failed", e);
        }
    }

    private String sanitize(String value) {
        return value
                .replaceAll("[^a-zA-Z0-9._@-]", "_")
                .replace("@", "_at_");
    }

    public record StoredComposeFile(
            String fileId,
            String objectKey,
            long size,
            String downloadUrl
    ) {
    }
}