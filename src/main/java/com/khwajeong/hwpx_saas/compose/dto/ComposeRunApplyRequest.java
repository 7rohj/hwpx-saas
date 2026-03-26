package com.khwajeong.hwpx_saas.compose.dto;

public class ComposeRunApplyRequest {
    private String sessionId;
    private String label; // heading1, heading2 ...

    // =========================
    // 추가:
    // 폰트 정책
    // - original : 템플릿 폰트 그대로 유지
    // - safe     : 안전 폰트로 치환
    // =========================
    private String fontPolicy;

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

    public String getFontPolicy() {
        return fontPolicy;
    }

    public void setFontPolicy(String fontPolicy) {
        this.fontPolicy = fontPolicy;
    }
}