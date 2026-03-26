package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ComposeSessionService {

    public static class SessionData {
        private String sessionId;
        private Path templatePath;
        private Path sourcePath;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public Path getTemplatePath() { return templatePath; }
        public void setTemplatePath(Path templatePath) { this.templatePath = templatePath; }

        public Path getSourcePath() { return sourcePath; }
        public void setSourcePath(Path sourcePath) { this.sourcePath = sourcePath; }
    }

    private final Map<String, SessionData> store = new ConcurrentHashMap<>();

    public SessionData create(String sessionId, Path templatePath, Path sourcePath) {
        SessionData data = new SessionData();
        data.setSessionId(sessionId);
        data.setTemplatePath(templatePath);
        data.setSourcePath(sourcePath);

        store.put(sessionId, data);
        return data;
    }

    public SessionData get(String sessionId) {
        return store.get(sessionId);
    }

    public String createSessionId() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String random = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "cmp_" + timestamp + "_" + random;
    }
}