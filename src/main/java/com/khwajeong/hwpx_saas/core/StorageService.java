/*
package com.khwajeong.hwpx_saas.core;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class StorageService {

    private final Path root = Path.of("src/main/java/com/khwajeong/hwpx_saas/storage");
    private final Path templatesDir = root.resolve("templates");
    private final Path outputsDir = root.resolve("outputs");

    public StorageService() throws Exception {
        Files.createDirectories(templatesDir);
        Files.createDirectories(outputsDir);
    }

    public Path saveTemplate(String originalName, byte[] bytes) throws Exception {
        String safe = safeName(originalName);
        String stored = timePrefix() + "_" + safe;
        Path p = templatesDir.resolve(stored);
        Files.write(p, bytes);
        return p;
    }

    public Path saveOutput(String originalName, byte[] bytes) throws Exception {
        String safe = safeName(originalName);
        String stored = timePrefix() + "_" + safe;
        Path p = outputsDir.resolve(stored);
        Files.write(p, bytes);
        return p;
    }

    public Path findTemplate(String templateId) {
        return templatesDir.resolve(templateId);
    }

    public Path findOutput(String fileId) {
        return outputsDir.resolve(fileId);
    }

    private String timePrefix() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "file.hwpx";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
*/