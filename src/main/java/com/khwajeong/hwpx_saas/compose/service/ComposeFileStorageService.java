package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ComposeFileStorageService {

    private final Path uploadRoot;

    public ComposeFileStorageService(@Value("${compose.upload-dir:uploads/compose}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + this.uploadRoot, e);
        }
    }

    public Path save(MultipartFile file, String prefix) {
        validate(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = getExtension(originalFilename);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String storedName = prefix + "_" + timestamp + "_" + UUID.randomUUID() + ext;

        Path target = uploadRoot.resolve(storedName).normalize();

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + originalFilename, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Original filename is missing.");
        }

        String lower = filename.toLowerCase();
        if (!(lower.endsWith(".hwpx") || lower.endsWith(".hwp"))) {
            throw new IllegalArgumentException("Only .hwpx or .hwp files are allowed.");
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return filename.substring(idx);
    }
}