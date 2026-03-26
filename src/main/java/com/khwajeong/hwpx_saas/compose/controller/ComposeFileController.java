package com.khwajeong.hwpx_saas.compose.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/compose/files")
public class ComposeFileController {

    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) throws IOException {
        // 주의:
        // apply 단계에서 Files.createTempFile("compose-output-", ".hwpx") 로 만들었으므로
        // 시스템 temp 디렉토리 아래 파일명을 기준으로 다시 찾아야 함
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path filePath = tempDir.resolve(fileId);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("file not found: " + fileId);
        }

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(filePath))
                .body(resource);
    }
}