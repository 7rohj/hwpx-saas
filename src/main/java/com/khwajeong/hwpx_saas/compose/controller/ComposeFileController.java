package com.khwajeong.hwpx_saas.compose.controller;

import com.khwajeong.hwpx_saas.service.R2StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compose/files")
public class ComposeFileController {

    private final R2StorageService r2StorageService;

    public ComposeFileController(R2StorageService r2StorageService) {
        this.r2StorageService = r2StorageService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(
            @PathVariable String fileId,
            @RequestParam(value = "userEmail", required = false) String userEmail
    ) {
        String safeEmail = sanitize(
                userEmail == null || userEmail.isBlank()
                        ? "anonymous"
                        : userEmail
        );

        String safeFileId = sanitize(fileId);

        String objectKey = "compose/" + safeEmail + "/" + safeFileId;

        byte[] bytes = r2StorageService.download(objectKey);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFileId + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(bytes);
    }

    private String sanitize(String value) {
        return value
                .replaceAll("[^a-zA-Z0-9._@-]", "_")
                .replace("@", "_at_");
    }
}