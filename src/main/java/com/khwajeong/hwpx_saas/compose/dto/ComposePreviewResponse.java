package com.khwajeong.hwpx_saas.compose.dto;

public class ComposePreviewResponse {
    private String sessionId;
    private String templateOriginalFilename;
    private String sourceOriginalFilename;
    private String templateSavedPath;
    private String sourceSavedPath;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTemplateOriginalFilename() {
        return templateOriginalFilename;
    }

    public void setTemplateOriginalFilename(String templateOriginalFilename) {
        this.templateOriginalFilename = templateOriginalFilename;
    }

    public String getSourceOriginalFilename() {
        return sourceOriginalFilename;
    }

    public void setSourceOriginalFilename(String sourceOriginalFilename) {
        this.sourceOriginalFilename = sourceOriginalFilename;
    }

    public String getTemplateSavedPath() {
        return templateSavedPath;
    }

    public void setTemplateSavedPath(String templateSavedPath) {
        this.templateSavedPath = templateSavedPath;
    }

    public String getSourceSavedPath() {
        return sourceSavedPath;
    }

    public void setSourceSavedPath(String sourceSavedPath) {
        this.sourceSavedPath = sourceSavedPath;
    }
}