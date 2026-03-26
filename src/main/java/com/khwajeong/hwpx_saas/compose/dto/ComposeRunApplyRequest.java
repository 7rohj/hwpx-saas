package com.khwajeong.hwpx_saas.compose.dto;

public class ComposeRunApplyRequest {
    private String sessionId;
    private String label; // heading1, heading2 ...

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}